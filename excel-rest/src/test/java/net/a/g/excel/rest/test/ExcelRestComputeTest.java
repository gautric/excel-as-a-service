package net.a.g.excel.rest.test;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static net.a.g.excel.rest.test.ApiFixtures.API;
import static net.a.g.excel.rest.test.ApiFixtures.BASE;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;

/**
 * Compute endpoints on KYCAPI/ComputeKYC (score = 50*PEP + country + amount
 * bracket): URI templates, GET compute by names, POST compute per sheet and
 * the legacy POST /compute.
 */
@QuarkusTest
@TestProfile(Profile2.class)
public class ExcelRestComputeTest {

	private static final String COMPUTE = API + "/KYCAPI/sheet/ComputeKYC/compute";
	private static final String COMPUTE_HREF = BASE + "/KYCAPI/sheet/ComputeKYC/compute";
	private static final String TEMPLATE = COMPUTE_HREF + "/SCORE/PEP/{PEP}/COUNTRY/{COUNTRY}/AMOUNT/{AMOUNT}";
	private static final String LEGACY = API + "/compute";

	private static io.restassured.response.ValidatableResponse post(String path, String json) {
		return given().contentType(ContentType.JSON).body(json).when().post(path).then();
	}

	// ---------------------------------------------------------------- templates

	@Test
	public void templatesListOneUriTemplatePerOutput() {
		when().get(COMPUTE).then().statusCode(200).contentType(ContentType.JSON)
				.body("_links.rel", contains("uri-template-score", "sheet", "self"),
						"_links.find { it.rel == 'uri-template-score' }.href", is(TEMPLATE),
						"_links.find { it.rel == 'sheet' }.href", is(BASE + "/KYCAPI/sheet/ComputeKYC"),
						"_links.find { it.rel == 'self' }.href", is(COMPUTE_HREF));
	}

	@Test
	public void sheetWithoutApiHasNoTemplate() {
		when().get(API + "/KYCAPI/sheet/COUNTRY/compute").then().statusCode(200).body("_links.rel",
				contains("sheet", "self"));
	}

	@Test
	public void expandedTemplateComputes() {
		String template = when().get(COMPUTE).then().statusCode(200).extract()
				.path("_links.find { it.rel == 'uri-template-score' }.href");
		String url = template.replace("{PEP}", "true").replace("{COUNTRY}", "CY").replace("{AMOUNT}", "1000000")
				.replace("http://localhost:8081", "");
		when().get(url).then().statusCode(200).body("cells[0].value", is(125.0F));
	}

	// ---------------------------------------------------------------- GET compute by names

	@ParameterizedTest(name = "PEP={0} COUNTRY={1} AMOUNT={2} -> {3}")
	@CsvSource({ "false, FR, 0, 0.0", "true, FR, 0, 50.0", "false, KP, 0, 100.0", "false, FR, 99999, 0.0",
			"false, FR, 999999, 25.0", "false, FR, 999999999, 100.0", "true, RU, 10000000, 175.0",
			"TRUE, JP, 100000, 75.0" })
	public void computeByPathNames(String pep, String country, String amount, float score) {
		String path = "/SCORE/PEP/" + pep + "/COUNTRY/" + country + "/AMOUNT/" + amount;
		when().get(COMPUTE + path).then().statusCode(200).contentType(ContentType.JSON)
				.body("_count", is(1), "cells[0].address", is("ComputeKYC!C6"), "cells[0].value", is(score),
						"cells[0].type", is("NUMERIC"), "cells[0].metadata", is("@output(SCORE)"),
						"cells[0]._links[0].rel", is("uri-template"), "cells[0]._links[0].href", is(TEMPLATE),
						"_links.find { it.rel == 'self' }.href", is(COMPUTE_HREF + path));
	}

	@Test
	public void pathInputOrderDoesNotMatter() {
		when().get(COMPUTE + "/SCORE/AMOUNT/1000000/COUNTRY/CY/PEP/true").then().statusCode(200)
				.body("cells[0].value", is(125.0F));
	}

	@Test
	public void pathWithSubsetOfInputsKeepsWorkbookDefaults() {
		when().get(COMPUTE + "/SCORE/COUNTRY/RU").then().statusCode(200).body("cells[0].value", is(50.0F));
	}

	@Test
	public void pathOutputsMayBeCellReferences() {
		when().get(COMPUTE + "/C2,C3,C6/PEP/true/COUNTRY/KP").then().statusCode(200).body("_count", is(3),
				"cells.address", contains("ComputeKYC!C2", "ComputeKYC!C3", "ComputeKYC!C6"),
				"cells.value", contains(50.0F, 100.0F, 150.0F));
	}

	@Test
	public void unknownCountryGivesExcelError() {
		when().get(COMPUTE + "/SCORE/COUNTRY/XX").then().statusCode(200).body("cells[0].type", is("ERROR"));
	}

	@Test
	public void unknownPathOutputIsBadRequest() {
		when().get(COMPUTE + "/NOPE/PEP/true").then().statusCode(400).contentType(ContentType.JSON)
				.body("code", is("400"), "message", containsString("Unknown output 'NOPE'"));
	}

	@ParameterizedTest(name = "bad path value {0}")
	@CsvSource(delimiter = '|', value = { "AMOUNT/abc|Expected a numeric value", "COUNTRY/=1|formula injection",
			"COUNTRY/@x|formula injection" })
	public void badPathValueIsBadRequestWithoutStackTrace(String input, String fragment) {
		when().get(COMPUTE + "/SCORE/" + input).then().statusCode(400).contentType(ContentType.JSON)
				.body("code", is("400"), "message", containsString(fragment),
						"message", not(containsString("at net.a.g")));
	}

	@Test
	public void oddPathIsJsonBadRequest() {
		when().get(COMPUTE + "/SCORE/PEP/true/COUNTRY").then().statusCode(400).contentType(ContentType.JSON)
				.body("code", is("400"));
	}

	// ---------------------------------------------------------------- POST {resource}/sheet/{sheet}/compute

	@ParameterizedTest(name = "POST {0} -> {1}")
	@CsvSource(delimiter = '|', value = {
			"{\"inputs\":{\"PEP\":\"true\",\"COUNTRY\":\"KP\",\"AMOUNT\":\"100000000\"}}|250.0",
			"{\"inputs\":{\"PEP\":false,\"COUNTRY\":\"RU\"},\"outputs\":[\"SCORE\"]}|50.0",
			"{\"inputs\":{\"B2\":\"true\",\"B3\":\"CY\",\"B4\":\"1e6\"}}|125.0",
			"{\"inputs\":{\"ComputeKYC!B2\":\"true\",\"ComputeKYC!B3\":\"JP\"}}|50.0",
			"{\"inputs\":{\"PEP\":true,\"B3\":\"KP\"}}|150.0",
			"{}|0.0" })
	public void postComputeByNamesOrReferences(String body, float score) {
		post(COMPUTE, body).statusCode(200).contentType(ContentType.JSON).body("_count", is(1),
				"cells[0].address", is("ComputeKYC!C6"), "cells[0].value", is(score),
				"_links.find { it.rel == 'self' }.href", is(COMPUTE_HREF));
	}

	@Test
	public void postOutputsKeepRequestedOrder() {
		post(COMPUTE, "{\"inputs\":{\"PEP\":true,\"COUNTRY\":\"CY\",\"AMOUNT\":1000000},"
				+ "\"outputs\":[\"C6\",\"C4\",\"ComputeKYC!C3\",\"C2\"]}").statusCode(200).body("_count", is(4),
						"cells.address", contains("ComputeKYC!C6", "ComputeKYC!C4", "ComputeKYC!C3", "ComputeKYC!C2"),
						"cells.value", contains(125.0F, 50.0F, 25.0F, 50.0F));
	}

	@Test
	public void postOutputMayBeInAnotherSheet() {
		post(COMPUTE, "{\"outputs\":[\"COUNTRY!B5\",\"AMOUNT!A4\"]}").statusCode(200).body("cells.address",
				contains("COUNTRY!B5", "AMOUNT!A4"), "cells.value", contains(100.0F, 10000000.0F));
	}

	@Test
	public void postEchoesRequestUuid() {
		post(COMPUTE, "{\"uuid\":\"req-42\",\"inputs\":{\"PEP\":true}}").statusCode(200).body("uuid",
				is("req-42"));
	}

	@Test
	public void pathResourceAndSheetWinOverBody() {
		post(COMPUTE, "{\"resource\":\"NOPE\",\"sheet\":\"NOPE\",\"inputs\":{\"PEP\":true}}").statusCode(200)
				.body("cells[0].value", is(50.0F));
	}

	@ParameterizedTest(name = "global={0}")
	@ValueSource(strings = { "true", "false" })
	public void globalFlagDoesNotChangeLocalResult(String global) {
		post(COMPUTE, "{\"global\":" + global + ",\"inputs\":{\"PEP\":true,\"COUNTRY\":\"CY\",\"AMOUNT\":1000000}}")
				.statusCode(200).body("cells[0].value", is(125.0F));
	}

	@Test
	public void unknownJsonPropertyIsIgnored() {
		post(COMPUTE, "{\"foo\":1,\"inputs\":{\"PEP\":true}}").statusCode(200).body("cells[0].value", is(50.0F));
	}

	@Test
	public void postWithoutBodyReturnsEveryOutput() {
		given().contentType(ContentType.JSON).when().post(COMPUTE).then().statusCode(200).body("_count", is(1),
				"cells[0].value", is(0.0F));
	}

	@ParameterizedTest(name = "POST {0} -> 400")
	@CsvSource(delimiter = '|', value = { "{\"outputs\":[\"\"]}|Empty output name",
			"{\"inputs\":{\"NOPE\":\"1\"}}|Unknown input 'NOPE'",
			"{\"outputs\":[\"c6\"]}|Unknown output 'c6'", "{\"inputs\":{\"COUNTRY\":\"=HYPERLINK(1)\"}}|formula injection",
			"{\"inputs\":{\"B4\":\"12abc\"}}|Expected a numeric value" })
	public void invalidComputeRequestIsJsonBadRequest(String body, String fragment) {
		post(COMPUTE, body).statusCode(400).contentType(ContentType.JSON).body("code", is("400"), "message",
				containsString(fragment), "message", not(containsString("at net.a.g")), "cells", nullValue());
	}

	@ParameterizedTest(name = "malformed body {0}")
	@ValueSource(strings = { "{", "{\"inputs\":\"x\"}" })
	public void malformedJsonIsBadRequest(String body) {
		post(COMPUTE, body).statusCode(400).body(not(containsString("at com.fasterxml")));
	}

	@Test
	public void malformedJsonIsJsonErrorBody() {
		post(COMPUTE, "{").statusCode(400).contentType(ContentType.JSON).body("code", is("400"));
	}

	@Test
	public void nonJsonBodyIsUnsupportedMediaType() {
		given().contentType(ContentType.TEXT).body("PEP=true").when().post(COMPUTE).then().statusCode(415)
				.body("code", is("415"));
	}

	// ---------------------------------------------------------------- legacy POST /compute

	@Test
	public void legacyComputeWithNames() {
		post(LEGACY, "{\"resource\":\"KYCAPI\",\"sheet\":\"ComputeKYC\","
				+ "\"inputs\":{\"PEP\":true,\"COUNTRY\":\"CY\",\"AMOUNT\":1000000},\"outputs\":[\"SCORE\"]}")
				.statusCode(200).contentType(ContentType.JSON)
				.body("_count", is(1), "cells[0].value", is(125.0F),
						"_links.find { it.rel == 'self' }.href", is(BASE + "/compute"));
	}

	@Test
	public void legacyComputeWithAbsoluteReferencesNeedsNoSheet() {
		post(LEGACY, "{\"resource\":\"KYCAPI\",\"inputs\":{\"ComputeKYC!B2\":\"true\",\"ComputeKYC!B3\":\"RU\"},"
				+ "\"outputs\":[\"ComputeKYC!C6\"]}").statusCode(200).body("cells[0].value", is(100.0F));
	}

	@Test
	public void legacyComputeWithoutOutputsReturnsEveryOutputOfTheSheet() {
		post(LEGACY, "{\"resource\":\"KYCAPI\",\"sheet\":\"ComputeKYC\",\"inputs\":{\"PEP\":true}}").statusCode(200)
				.body("cells[0].metadata", is("@output(SCORE)"), "cells[0].value", is(50.0F));
	}

	@Test
	public void legacyComputeEchoesUuid() {
		post(LEGACY, "{\"uuid\":\"legacy-1\",\"resource\":\"KYCAPI\",\"sheet\":\"ComputeKYC\"}").statusCode(200)
				.body("uuid", is("legacy-1"));
	}

	@ParameterizedTest(name = "legacy {0} -> {1}")
	@CsvSource(delimiter = '|', value = {
			"{\"sheet\":\"ComputeKYC\"}|400|Resource name cannot be null or empty",
			"{\"resource\":\"../etc\",\"outputs\":[\"A1\"]}|400|Invalid resource name",
			"{\"resource\":\"NOPE\",\"outputs\":[\"Sheet!A1\"]}|404|NOPE",
			"{\"resource\":\"KYCAPI\",\"sheet\":\"NOPE\"}|404|Sheet 'NOPE' not found in resource 'KYCAPI'",
			"{\"resource\":\"KYCAPI\",\"outputs\":[\"SCORE\"]}|400|use 'Sheet!A1' or set 'sheet'",
			"{\"resource\":\"KYCAPI\"}|400|At least one output is required" })
	public void legacyComputeErrors(String body, int status, String fragment) {
		post(LEGACY, body).statusCode(status).contentType(ContentType.JSON).body("code", is("" + status),
				"message", containsString(fragment));
	}

	@Test
	public void legacyComputeWithoutBodyIsBadRequest() {
		given().contentType(ContentType.JSON).when().post(LEGACY).then().statusCode(400).body("message",
				is("JSON body is required"));
	}

	@Test
	public void computeResultsAreIndependentAcrossEndpoints() {
		post(LEGACY, "{\"resource\":\"KYCAPI\",\"sheet\":\"ComputeKYC\",\"inputs\":{\"COUNTRY\":\"KP\"}}")
				.statusCode(200).body("cells[0].value", is(100.0F));
		when().get(COMPUTE + "/SCORE/PEP/true").then().statusCode(200).body("cells[0].value", is(50.0F));
		post(COMPUTE, "{}").statusCode(200).body("cells", hasSize(1), "cells[0].value", is(0.0F));
	}
}
