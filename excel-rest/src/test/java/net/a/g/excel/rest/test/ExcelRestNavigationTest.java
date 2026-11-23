package net.a.g.excel.rest.test;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static net.a.g.excel.rest.test.ApiFixtures.API;
import static net.a.g.excel.rest.test.ApiFixtures.BASE;
import static net.a.g.excel.rest.test.ApiFixtures.XLSX;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;

/**
 * Read-only navigation of the KYCAPI resource: list, resource, sheets, sheet,
 * cells, cell (with query inputs), HATEOAS links, headers and error answers.
 */
@QuarkusTest
@TestProfile(Profile2.class)
public class ExcelRestNavigationTest {

	private static final String SHEET = API + "/KYCAPI/sheet/ComputeKYC";
	private static final String SHEET_HREF = BASE + "/KYCAPI/sheet/ComputeKYC";

	// ---------------------------------------------------------------- list

	@Test
	public void listIsJsonWithSelfLinkAndLinkHeader() {
		when().get(API).then().statusCode(200).contentType(ContentType.JSON)
				.header("Link", containsString("rel=\"self\""))
				.body("_links.find { it.rel == 'self' }.href", is(BASE));
	}

	@Test
	public void listContainsKycapiAndCountMatchesEntries() {
		io.restassured.path.json.JsonPath json = when().get(API).then().statusCode(200)
				.body("resources.name", hasItem("KYCAPI"),
						"resources.find { it.name == 'KYCAPI' }.file", is("KYCAPI.xlsx"))
				.extract().jsonPath();
		org.junit.jupiter.api.Assertions.assertEquals(json.getList("resources").size(), json.getInt("_count"));
	}

	@ParameterizedTest(name = "resource link {0}")
	@CsvSource({ "list-of-resource, '', application/json", "self, /KYCAPI, application/json",
			"download, /KYCAPI/_download, " + XLSX, "list-of-sheet, /KYCAPI/sheets, application/json" })
	public void listedResourceCarriesHateoasLinks(String rel, String suffix, String type) {
		String link = "resources.find { it.name == 'KYCAPI' }._links.find { it.rel == '" + rel + "' }";
		when().get(API).then().statusCode(200).body(link + ".href", is(BASE + suffix), link + ".type", is(type));
	}

	// ---------------------------------------------------------------- resource

	@Test
	public void resourceHasNameFileAndSelfLinkHeader() {
		when().get(API + "/KYCAPI").then().statusCode(200).contentType(ContentType.JSON)
				.header("Link", containsString(BASE + "/KYCAPI"))
				.body("name", is("KYCAPI"), "file", is("KYCAPI.xlsx"), "_links", hasSize(4),
						"_links.rel", contains("list-of-resource", "self", "download", "list-of-sheet"));
	}

	@Test
	public void resourceNeverExposesTheBinaryDocument() {
		when().get(API + "/KYCAPI").then().statusCode(200).body("doc", nullValue());
	}

	@Test
	public void resourceNameIsCaseSensitive() {
		when().get(API + "/kycapi").then().statusCode(404).body("message", is("Resource 'kycapi' Not Found"));
	}

	@ParameterizedTest(name = "unknown resource: {0}")
	@ValueSource(strings = { "/NOPE/sheets", "/NOPE/sheet/ComputeKYC", "/NOPE/sheet/ComputeKYC/cell/C6",
			"/NOPE/sheet/ComputeKYC/cells/C2,C6", "/NOPE/sheet/ComputeKYC/compute/SCORE/PEP/true", "/NOPE/_download" })
	public void unknownResourceIsJsonNotFound(String path) {
		given().urlEncodingEnabled(false).when().get(API + path).then().statusCode(404).contentType(ContentType.JSON)
				.body("code", is("404"), "message", is("Resource 'NOPE' Not Found"));
	}

	@ParameterizedTest(name = "unknown sheet: {0}")
	@ValueSource(strings = { "", "/cell/C6", "/cells/C2,C6", "/compute/SCORE/PEP/true" })
	public void unknownSheetIsJsonNotFound(String suffix) {
		given().urlEncodingEnabled(false).when().get(API + "/KYCAPI/sheet/NOPE" + suffix).then().statusCode(404).contentType(ContentType.JSON)
				.body("code", is("404"), "message", containsString("Sheet 'NOPE' Not Found in Resource 'KYCAPI'"));
	}

	@Test
	public void unmatchedRouteIsJsonNotFound() {
		when().get(API + "/KYCAPI/sheet").then().statusCode(404).contentType(ContentType.JSON).body("code",
				is("404"));
	}

	@ParameterizedTest(name = "{0} {1} is not allowed")
	@CsvSource({ "PATCH, /KYCAPI", "POST, /KYCAPI/sheets", "PUT, /KYCAPI/sheet/ComputeKYC/compute" })
	public void wrongMethodIsMethodNotAllowed(String method, String path) {
		given().when().request(method, API + path).then().statusCode(405).body("code", is("405"));
	}

	// ---------------------------------------------------------------- sheets

	@Test
	public void sheetsAreListedInWorkbookOrder() {
		when().get(API + "/KYCAPI/sheets").then().statusCode(200).contentType(ContentType.JSON)
				.body("_count", is(3), "sheets.name", contains("ComputeKYC", "COUNTRY", "AMOUNT"),
						"_links.find { it.rel == 'self' }.href", is(BASE + "/KYCAPI/sheets"));
	}

	@ParameterizedTest(name = "sheet links of {0}")
	@ValueSource(strings = { "ComputeKYC", "COUNTRY", "AMOUNT" })
	public void eachListedSheetLinksToItsResourceCellsAndTemplates(String sheet) {
		String links = "sheets.find { it.name == '" + sheet + "' }._links";
		String href = BASE + "/KYCAPI/sheet/" + sheet;
		when().get(API + "/KYCAPI/sheets").then().statusCode(200)
				.body(links + ".rel", contains("resource", "self", "list-of-cell", "list-of-template"),
						links + ".href", contains(BASE + "/KYCAPI", href, href + "/cells", href + "/compute"));
	}

	// ---------------------------------------------------------------- sheet

	@ParameterizedTest(name = "sheet {0}")
	@ValueSource(strings = { "ComputeKYC", "COUNTRY", "AMOUNT" })
	public void sheetIsFoundWithSelfLink(String sheet) {
		when().get(API + "/KYCAPI/sheet/" + sheet).then().statusCode(200).contentType(ContentType.JSON)
				.header("Link", containsString("/KYCAPI/sheet/" + sheet))
				.body("name", is(sheet), "_links.find { it.rel == 'self' }.href", is(BASE + "/KYCAPI/sheet/" + sheet));
	}

	@Test
	public void sheetLookupIsCaseInsensitiveLikeExcel() {
		when().get(API + "/KYCAPI/sheet/computekyc").then().statusCode(200).body("name", is("computekyc"));
	}

	// ---------------------------------------------------------------- cells

	@Test
	public void cellsOfComputeSheetCarryApiMetadata() {
		when().get(SHEET + "/cells").then().statusCode(200).contentType(ContentType.JSON)
				.body("_count", is(15), "cells", hasSize(15),
						"cells.find { it.address == 'ComputeKYC!B2' }.metadata", is("@input(PEP)"),
						"cells.find { it.address == 'ComputeKYC!B3' }.metadata", is("@input(COUNTRY)"),
						"cells.find { it.address == 'ComputeKYC!B4' }.metadata", is("@input(AMOUNT)"),
						"cells.findAll { it.metadata != null }", hasSize(4),
						"cells.find { it.address == 'ComputeKYC!C6' }.metadata", is("@output(SCORE)"));
	}

	@Test
	public void everyListedCellLinksToItself() {
		when().get(SHEET + "/cells").then().statusCode(200)
				.body("cells.findAll { it._links.size() == 3 }", hasSize(15),
						"cells.find { it.address == 'ComputeKYC!B4' }._links.find { it.rel == 'self' }.href",
						is(SHEET_HREF + "/cell/B4"));
	}

	@ParameterizedTest(name = "{0} cell {1} = {2} ({3})")
	@CsvSource({ "COUNTRY, A3, CY, STRING", "AMOUNT, A3, A2*10, FORMULA" })
	public void cellsOfLookupSheets(String sheet, String cell, String value, String type) {
		String c = "cells.find { it.address == '" + sheet + "!" + cell + "' }";
		when().get(API + "/KYCAPI/sheet/" + sheet + "/cells").then().statusCode(200).body("_count", is(10),
				c + ".value.toString()", is(value), c + ".type", is(type));
	}

	// ---------------------------------------------------------------- cell

	@ParameterizedTest(name = "raw {0} = {1} ({2})")
	@CsvSource({ "A2, PEP, STRING, ", "B2, false, BOOLEAN, @input(PEP)", "B3, FR, STRING, @input(COUNTRY)",
			"B4, 0.0, NUMERIC, @input(AMOUNT)",
			"C3, 'VLOOKUP(B3,COUNTRY!A1:B5,2,FALSE)', FORMULA, ",
			"C6, SUM(C2:C4), FORMULA, @output(SCORE)", "B5, '', BLANK, " })
	public void rawCellWithoutQueryIsNotEvaluated(String cell, String value, String type, String metadata) {
		when().get(SHEET + "/cell/" + cell).then().statusCode(200).contentType(ContentType.JSON)
				.body("address", is("ComputeKYC!" + cell), "value.toString()", is(value), "type", is(type),
						"metadata", is(metadata), "_links.rel", contains("resource", "sheet", "self"),
						"_links.find { it.rel == 'self' }.href", is(SHEET_HREF + "/cell/" + cell));
	}

	@ParameterizedTest(name = "C6 with {0} = {1}")
	@CsvSource(delimiter = '|', value = { "B2=true|50.0", "B2=TRUE&B3=CY|75.0", "B3=JP|0.0", "B4=100000|25.0",
			"B2=true&B3=KP&B4=100000000|250.0", "ComputeKYC!B2=true&B4=1e6|100.0" })
	public void cellIsComputedWithQueryInputs(String query, float score) {
		when().get(SHEET + "/cell/C6?" + query).then().statusCode(200)
				.body("value", is(score), "type", is("NUMERIC"), "metadata", is("@output(SCORE)"));
	}

	@Test
	public void queriedCellAddsQueryLink() {
		when().get(SHEET + "/cell/C6?B3=CY").then().statusCode(200).body("_links", hasSize(4),
				"_links.find { it.rel == 'query' }.href", is(SHEET_HREF + "/cell/C6?B3=CY"));
	}

	@Test
	public void queryInputsDoNotLeakIntoNextRequest() {
		when().get(SHEET + "/cell/C6?B2=true&B3=KP").then().statusCode(200).body("value", is(150.0F));
		when().get(SHEET + "/cell/B2").then().statusCode(200).body("value", is("false"));
		when().get(SHEET + "/cell/C6?B4=0").then().statusCode(200).body("value", is(0.0F));
	}

	@ParameterizedTest(name = "invalid cell {0}")
	@ValueSource(strings = { "c6", "6C", "ABCD1", "Z999" })
	public void invalidOrMissingCellIsJsonNotFound(String cell) {
		when().get(SHEET + "/cell/" + cell).then().statusCode(404).contentType(ContentType.JSON)
				.body("code", is("404"), "message", is("Cell '" + cell + "' Not Found in Sheet 'ComputeKYC'"));
	}

	@ParameterizedTest(name = "bad query {0}={1}")
	@CsvSource(delimiter = '|', value = { "B4|abc|Expected a numeric value", "B3|=1+1|formula injection",
			"B4|-5|formula injection",
			"foo|1|Invalid cell reference" })
	public void badQueryInputIsJsonBadRequestWithoutStackTrace(String name, String value, String fragment) {
		given().queryParam(name, value).when().get(SHEET + "/cell/C6").then().statusCode(400).contentType(ContentType.JSON)
				.body("code", is("400"), "message", containsString(fragment), "message",
						not(containsString("at net.a.g")));
	}

	@ParameterizedTest(name = "input cell {0} outside the workbook")
	@ValueSource(strings = { "NOSHEET!B2", "B99" })
	public void inputCellOutsideTheWorkbookIsClientError(String input) {
		given().queryParam(input, "1").when().get(SHEET + "/cell/C6").then().statusCode(400).body("code", is("400"));
	}

	@Test
	public void globalQueryFlagIsAccepted() {
		when().get(SHEET + "/cell/C6?B2=true&_global=true").then().statusCode(200).body("value", is(50.0F));
	}

	// ---------------------------------------------------------------- cells/{a,b}

	@Test
	public void cellListWithoutQueryReturnsRawFormulas() {
		given().urlEncodingEnabled(false).when().get(SHEET + "/cells/C2,C6").then().statusCode(200).body("_count", is(2),
				"cells.address", contains("ComputeKYC!C2", "ComputeKYC!C6"),
				"cells.value", contains("IF(B2,50,0)", "SUM(C2:C4)"));
	}

	@Test
	public void cellListIsComputedWithQueryInputs() {
		given().urlEncodingEnabled(false).when().get(SHEET + "/cells/C2,C3,C4,C6?B2=true&B3=CY&B4=1000000").then().statusCode(200)
				.body("_count", is(4), "cells.value", contains(50.0F, 25.0F, 50.0F, 125.0F),
						"cells[0]._links.find { it.rel == 'query' }.href", startsWith(SHEET_HREF + "/cells/"));
	}

	@Test
	public void cellListAcceptsOtherSheetReference() {
		given().urlEncodingEnabled(false).when().get(SHEET + "/cells/AMOUNT!B3,C6").then().statusCode(200).body("_count", is(2),
				"cells.address", contains("AMOUNT!B3", "ComputeKYC!C6"));
	}

	@Test
	public void cellListSkipsCellsOutsideTheSheet() {
		given().urlEncodingEnabled(false).when().get(SHEET + "/cells/C6,Z999").then().statusCode(200).body("_count", is(1),
				"cells[0].address", is("ComputeKYC!C6"));
	}

	@Test
	public void cellListWithInvalidReferenceIsBadRequest() {
		given().urlEncodingEnabled(false).when().get(SHEET + "/cells/c6,C2").then().statusCode(400).contentType(ContentType.JSON).body("code",
				is("400"), "message", containsString("c6"));
	}

	// ---------------------------------------------------------------- OpenAPI

	@Test
	public void openApiDocumentDescribesTheApi() {
		when().get("/swagger").then().statusCode(200).body(containsString("/eaas/api/{resource}/_download"),
				containsString("/eaas/api/compute"));
	}
}
