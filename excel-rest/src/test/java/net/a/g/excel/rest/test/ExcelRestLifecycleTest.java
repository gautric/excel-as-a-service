package net.a.g.excel.rest.test;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static net.a.g.excel.rest.test.ApiFixtures.API;
import static net.a.g.excel.rest.test.ApiFixtures.BASE;
import static net.a.g.excel.rest.test.ApiFixtures.XLSX;
import static net.a.g.excel.rest.test.ApiFixtures.delete;
import static net.a.g.excel.rest.test.ApiFixtures.sample;
import static net.a.g.excel.rest.test.ApiFixtures.unique;
import static net.a.g.excel.rest.test.ApiFixtures.upload;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;

/**
 * Create / replace / delete / download of resources, and computation on
 * uploaded workbooks. Every resource created here has a unique name and is
 * deleted afterwards, so the shared Profile2 repository is left as found.
 */
@QuarkusTest
@TestProfile(Profile2.class)
public class ExcelRestLifecycleTest {

	private static String test1;
	private static String types;

	// Quarkus only listens once the first test instance runs, hence lazy, not @BeforeAll.
	@BeforeEach
	void uploadFixtures() {
		if (test1 == null) {
			test1 = upload(unique("Test1"), "Test1.xlsx");
			types = upload(unique("Type"), "Type.xlsx");
		}
	}

	@AfterAll
	static void deleteFixtures() {
		try {
			delete(test1);
			delete(types);
		} catch (Exception appAlreadyStopped) {
			// the repository is in memory: once Quarkus is stopped there is nothing left to clean
		}
	}

	// ---------------------------------------------------------------- create

	@Test
	public void postCreatesWithLocationAndLinks() {
		String name = unique("Post");
		try {
			given().multiPart("file", "Post.xlsx", sample("KYC.xlsx")).when().post(API + "/" + name).then()
					.statusCode(201).contentType(ContentType.JSON).header("Location", is(BASE + "/" + name))
					.body("name", is(name), "file", is("Post.xlsx"),
							"_links.rel", contains("list-of-resource", "self", "download", "list-of-sheet"),
							"_links.find { it.rel == 'self' }.href", is(BASE + "/" + name));

			when().get(API + "/" + name).then().statusCode(200).body("file", is("Post.xlsx"));
			when().get(API).then().statusCode(200).body("resources.name", hasItem(name));
			when().get(API + "/" + name + "/sheets").then().statusCode(200).body("sheets.name",
					contains("ComputeKYC", "COUNTRY", "AMOUNT"));
		} finally {
			delete(name);
		}
	}

	@Test
	public void postOnExistingNameReplacesWithoutLocation() {
		String name = upload(unique("Repost"), "KYC.xlsx");
		try {
			given().multiPart("file", "Again.xlsx", sample("Test1.xlsx")).when().post(API + "/" + name).then()
					.statusCode(200).header("Location", nullValue()).body("file", is("Again.xlsx"));
		} finally {
			delete(name);
		}
	}

	@Test
	public void putCreatesThenReplacesContent() {
		String name = unique("Put");
		try {
			given().multiPart("file", "first.xlsx", sample("KYC.xlsx")).when().put(API + "/" + name).then()
					.statusCode(201).header("Location", is(BASE + "/" + name));
			when().get(API + "/" + name + "/sheets").then().body("_count", is(3));

			given().multiPart("file", "second.xlsx", sample("Test1.xlsx")).when().put(API + "/" + name).then()
					.statusCode(200).header("Location", nullValue()).body("name", is(name), "file",
							is("second.xlsx"));
			when().get(API + "/" + name + "/sheets").then().statusCode(200).body("sheets.name",
					contains("Sheet1", "Sheet2", "Sheet3"));
			when().get(API + "/" + name + "/sheet/ComputeKYC").then().statusCode(404);
		} finally {
			delete(name);
		}
	}

	@Test
	public void rejectedReplaceKeepsPreviousWorkbook() {
		String name = upload(unique("Keep"), "Test1.xlsx");
		try {
			String etag = when().get(API + "/" + name + "/_download").then().extract().header("ETag");
			given().multiPart("file", "x.txt", "not excel".getBytes()).when().put(API + "/" + name).then()
					.statusCode(400);
			when().get(API + "/" + name).then().statusCode(200).body("file", is(name + ".xlsx"));
			when().get(API + "/" + name + "/_download").then().statusCode(200).header("ETag", is(etag));
		} finally {
			delete(name);
		}
	}

	@ParameterizedTest(name = "invalid resource name ''{0}''")
	@ValueSource(strings = { "bad.name", "bad name", "café" })
	public void invalidResourceNameIsRejected(String name) {
		given().multiPart("file", "ok.xlsx", sample("Test1.xlsx")).when().post(API + "/{name}", name).then()
				.statusCode(400).contentType(ContentType.JSON)
				.body("code", is("400"), "message", containsString("Invalid resource name"));
		when().get(API + "/{name}", name).then().statusCode(404);
	}

	@Test
	public void tooLongResourceNameIsRejected() {
		given().multiPart("file", "ok.xlsx", sample("Test1.xlsx")).when().post(API + "/" + "x".repeat(256)).then()
				.statusCode(400).body("message", containsString("Invalid resource name"));
	}

	@ParameterizedTest(name = "non Excel payload: {0}")
	@CsvSource(delimiter = '|', value = { "empty|", "text|hello world", "zip|PK\u0003\u0004 truncated" })
	public void nonExcelPayloadIsRejectedAndNotStored(String kind, String content) {
		String name = unique("Bad_" + kind);
		byte[] bytes = content == null ? new byte[0] : content.getBytes(StandardCharsets.ISO_8859_1);
		given().multiPart("file", kind + ".xlsx", bytes).when().post(API + "/" + name).then().statusCode(400)
				.contentType(ContentType.JSON)
				.body("code", is("400"), "message", is("Server cannot accept/recognize format file provided"));
		when().get(API + "/" + name).then().statusCode(404);
	}

	@ParameterizedTest(name = "{0} without file field")
	@ValueSource(strings = { "POST", "PUT" })
	public void missingFileFieldIsBadRequest(String method) {
		String name = unique("NoFile");
		given().multiPart("other", "x").when().request(method, API + "/" + name).then().statusCode(400)
				.contentType(ContentType.JSON).body("message", is("Multipart field 'file' is required"));
		when().get(API + "/" + name).then().statusCode(404);
	}

	@Test
	public void uploadMustBeMultipart() {
		given().contentType(ContentType.JSON).body("{}").when().post(API + "/" + unique("Json")).then()
				.statusCode(415).body("code", is("415"));
	}

	// ---------------------------------------------------------------- delete

	@Test
	public void deleteRemovesEverySubResource() {
		String name = upload(unique("Del"), "KYC.xlsx");
		when().delete(API + "/" + name).then().statusCode(204).body(emptyString());

		for (String path : new String[] { "", "/sheets", "/sheet/ComputeKYC", "/sheet/ComputeKYC/cells",
				"/sheet/ComputeKYC/cell/C6", "/_download" }) {
			when().get(API + "/" + name + path).then().statusCode(404).body("code", is("404"));
		}
		when().get(API).then().body("resources.name", not(hasItem(name)));
		when().delete(API + "/" + name).then().statusCode(404).contentType(ContentType.JSON).body("message",
				is("Resource '" + name + "' Not Found"));
	}

	@Test
	public void deletedNameCanBeReused() {
		String name = upload(unique("Reuse"), "KYC.xlsx");
		try {
			when().delete(API + "/" + name).then().statusCode(204);
			given().multiPart("file", "again.xlsx", sample("Test1.xlsx")).when().post(API + "/" + name).then()
					.statusCode(201);
		} finally {
			delete(name);
		}
	}

	// ---------------------------------------------------------------- download

	@Test
	public void downloadReturnsTheWorkbookWithCachingHeaders() {
		byte[] body = when().get(API + "/KYCAPI/_download").then().statusCode(200).contentType(XLSX)
				.header("Content-Disposition", is("attachment; filename=\"KYCAPI.xlsx\""))
				.header("Cache-Control", containsString("no-cache"))
				.header("ETag", matchesPattern("\"[0-9a-f]{32}\"")).extract().asByteArray();
		assertArrayEquals(sample("KYCAPI.xlsx"), body);
	}

	@ParameterizedTest(name = "If-None-Match {0} -> {1}")
	@CsvSource({ "MATCH, 304", "'*', 304", "'\"0000\"', 200" })
	public void conditionalDownload(String ifNoneMatch, int status) {
		String etag = when().get(API + "/KYCAPI/_download").then().extract().header("ETag");
		String header = "MATCH".equals(ifNoneMatch) ? etag : ifNoneMatch;
		io.restassured.response.ValidatableResponse r = given().header("If-None-Match", header).when()
				.get(API + "/KYCAPI/_download").then().statusCode(status).header("ETag", is(etag));
		if (status == 304) {
			r.body(emptyString());
		}
	}

	@Test
	public void etagDependsOnContentOnly() {
		String a = upload(unique("EtagA"), "KYCAPI.xlsx");
		try {
			String kycapi = when().get(API + "/KYCAPI/_download").then().extract().header("ETag");
			String etagA = when().get(API + "/" + a + "/_download").then().extract().header("ETag");
			assertEquals(kycapi, etagA);

			given().multiPart("file", "new.xlsx", sample("Test1.xlsx")).when().put(API + "/" + a).then()
					.statusCode(200);
			String replaced = when().get(API + "/" + a + "/_download").then().statusCode(200)
					.header("Content-Disposition", containsString("new.xlsx")).extract().header("ETag");
			assertNotEquals(etagA, replaced);
			given().header("If-None-Match", etagA).when().get(API + "/" + a + "/_download").then().statusCode(200);
		} finally {
			delete(a);
		}
	}

	@Test
	public void uploadedBytesRoundTrip() {
		byte[] body = when().get(API + "/" + test1 + "/_download").then().statusCode(200)
				.header("Content-Disposition", is("attachment; filename=\"" + test1 + ".xlsx\"")).extract()
				.asByteArray();
		assertArrayEquals(sample("Test1.xlsx"), body);
	}

	// ---------------------------------------------------------------- uploaded workbooks

	@ParameterizedTest(name = "{0}!{1}?{2} = {3}")
	@CsvSource(delimiter = '|', value = { "Sheet2|A1|Sheet1!A1=5|52.0", 
			"Sheet3|A1|Sheet1!A1=1|13.0", "Sheet2|B1|Sheet1!B1=Bonjour|Bonjour World" })
	public void crossSheetFormulasOfUploadedWorkbook(String sheet, String cell, String query, String value) {
		when().get(API + "/" + test1 + "/sheet/" + sheet + "/cell/" + cell + "?" + query).then().statusCode(200)
				.body("value.toString()", is(value), "_links.find { it.rel == 'resource' }.href",
						is(BASE + "/" + test1));
	}

	@Test
	public void uploadedWorkbookWithoutApiHasNoTemplateAndNeedsOutputs() {
		when().get(API + "/" + test1 + "/sheet/Sheet2/compute").then().statusCode(200).body("_links.rel",
				contains("sheet", "self"));
		given().contentType(ContentType.JSON).body("{}").when().post(API + "/" + test1 + "/sheet/Sheet2/compute")
				.then().statusCode(400).body("message", is("At least one output is required"));
	}

	@Test
	public void uploadedWorkbookComputesWithCellReferences() {
		given().contentType(ContentType.JSON).body("{\"inputs\":{\"Sheet1!A1\":\"3\"},\"outputs\":[\"A1\",\"B1\"]}")
				.when().post(API + "/" + test1 + "/sheet/Sheet2/compute").then().statusCode(200)
				.body("cells.value", contains(32.0F, "Hello World"));
	}

	@ParameterizedTest(name = "Type!{0} is {1}")
	@CsvSource({ "A1, NUMERIC, 0.0", "B1, STRING, --", "A5, DATE, 2021-01-01", "C3, BLANK, ''",
			"A9, FORMULA, A1+A2+41" })
	public void cellTypesOfUploadedWorkbook(String cell, String type, String value) {
		when().get(API + "/" + types + "/sheet/Sheet1/cell/" + cell).then().statusCode(200).body("type", is(type),
				"value.toString()", is(value));
	}

	@ParameterizedTest(name = "Type {0} -> {1}")
	@CsvSource(delimiter = '|', value = { "{\"inputs\":{\"A1\":\"1\"},\"outputs\":[\"A9\"]}|200|43.0",
			"{\"inputs\":{\"A5\":\"2022-02-03\"},\"outputs\":[\"A5\"]}|200|2022-02-03",
			"{\"inputs\":{\"A5\":\"03/02/2022\"},\"outputs\":[\"A5\"]}|400|Expected a date with format yyyy-MM-dd" })
	public void typedInputsOfUploadedWorkbook(String body, int status, String expected) {
		io.restassured.response.ValidatableResponse r = given().contentType(ContentType.JSON).body(body).when()
				.post(API + "/" + types + "/sheet/Sheet1/compute").then().statusCode(status);
		if (status == 200) {
			r.body("cells[0].value.toString()", is(expected));
		} else {
			r.body("code", is("400"), "message", containsString(expected));
		}
	}

	// ---------------------------------------------------------------- cross-workbook

	@Test
	public void crossWorkbookReferenceWithGlobal() {
		String secondary = upload(unique("Sec"), "Secondary.xlsx", "Secondary.xlsx");
		String primary = upload(unique("Pri"), "Primary.xlsx");
		try {
			given().contentType(ContentType.JSON)
					.body("{\"resource\":\"" + primary + "\",\"sheet\":\"Feuil1\",\"inputs\":{\"C1\":\"0\"},"
							+ "\"outputs\":[\"A1\",\"B1\"]}")
					.when().post(API + "/compute").then().statusCode(200).body("cells.value", contains(42.0F, 44.0F));
		} finally {
			delete(primary);
			delete(secondary);
		}
	}

	@Test
	public void crossWorkbookReferenceWithoutGlobalIsServerErrorWithoutStackTrace() {
		String primary = upload(unique("Pri"), "Primary.xlsx");
		try {
			given().contentType(ContentType.JSON).body("{\"global\":false,\"inputs\":{\"C1\":\"0\"},\"outputs\":[\"A1\"]}")
					.when().post(API + "/" + primary + "/sheet/Feuil1/compute").then().statusCode(500)
					.contentType(ContentType.JSON)
					.body("code", is("500"), "message", is("Internal server error"));
		} finally {
			delete(primary);
		}
	}

	@Test
	public void globalComputeSurvivesTwoResourcesWithTheSameFileName() {
		String twin = upload(unique("Twin"), "KYCAPI.xlsx", "KYCAPI.xlsx");
		try {
			given().contentType(ContentType.JSON)
					.body("{\"resource\":\"KYCAPI\",\"sheet\":\"ComputeKYC\",\"inputs\":{\"PEP\":true}}").when()
					.post(API + "/compute").then().statusCode(200).body("cells[0].value", is(50.0F));
		} finally {
			delete(twin);
		}
	}
}
