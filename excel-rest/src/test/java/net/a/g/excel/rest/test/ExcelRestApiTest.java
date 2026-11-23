package net.a.g.excel.rest.test;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;

import java.io.File;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;

/**
 * RESTful surface: JSON compute per sheet, typed errors, conditional download,
 * create/replace/delete of resources.
 */
@QuarkusTest
@TestProfile(Profile2.class)
public class ExcelRestApiTest {

	private static final String COMPUTE = "/eaas/api/KYCAPI/sheet/ComputeKYC/compute";

	@Test
	public void computeWithJsonBody() {
		given().contentType(ContentType.JSON)
				.body("{\"inputs\":{\"PEP\":true,\"COUNTRY\":\"CY\",\"AMOUNT\":1000000},\"outputs\":[\"SCORE\"]}")
				.when().post(COMPUTE).then().statusCode(200)
				.body("_count", is(1), "cells[0].address", is("ComputeKYC!C6"), "cells[0].value", is(125.0F));
	}

	@Test
	public void computeWithoutOutputsReturnsEveryOutput() {
		given().contentType(ContentType.JSON).body("{\"inputs\":{\"PEP\":true}}").when().post(COMPUTE).then()
				.statusCode(200).body("_count", is(1), "cells[0].value", is(50.0F));
	}

	@Test
	public void computeIsStatelessAcrossRequests() {
		given().contentType(ContentType.JSON).body("{\"inputs\":{\"PEP\":true,\"COUNTRY\":\"CY\"}}").when()
				.post(COMPUTE).then().statusCode(200).body("cells[0].value", is(75.0F));
		given().contentType(ContentType.JSON).body("{\"inputs\":{\"PEP\":true}}").when().post(COMPUTE).then()
				.statusCode(200).body("cells[0].value", is(50.0F));
	}

	@Test
	public void unknownOutputIsBadRequest() {
		given().contentType(ContentType.JSON).body("{\"outputs\":[\"NOPE\"]}").when().post(COMPUTE).then()
				.statusCode(400).body("code", is("400"), "message", containsString("NOPE"));
	}

	@Test
	public void badNumberIsBadRequestWithoutStackTrace() {
		given().contentType(ContentType.JSON).body("{\"inputs\":{\"AMOUNT\":\"abc\"}}").when().post(COMPUTE).then()
				.statusCode(400).body("message", not(containsString("at net.a.g")));
	}

	@Test
	public void unknownSheetOrResourceIsNotFound() {
		given().contentType(ContentType.JSON).body("{}").when().post("/eaas/api/KYCAPI/sheet/NOPE/compute").then()
				.statusCode(404).body("code", is("404"));
		given().contentType(ContentType.JSON).body("{}").when().post("/eaas/api/NOPE/sheet/ComputeKYC/compute")
				.then().statusCode(404);
	}

	@Test
	public void downloadSupportsConditionalGet() {
		String etag = when().get("/eaas/api/KYCAPI/_download").then().statusCode(200)
				.header("ETag", notNullValue()).extract().header("ETag");

		given().header("If-None-Match", etag).when().get("/eaas/api/KYCAPI/_download").then().statusCode(304);
	}

	@Test
	public void createReplaceDeleteLifecycle() {
		File xlsx = new File("../sample/KYC.xlsx");

		given().multiPart("file", xlsx).when().post("/eaas/api/TMP").then().statusCode(201)
				.header("Location", endsWith("/eaas/api/TMP"));
		given().multiPart("file", xlsx).when().put("/eaas/api/TMP").then().statusCode(200);
		when().get("/eaas/api/TMP").then().statusCode(200);

		when().delete("/eaas/api/TMP").then().statusCode(204);
		when().delete("/eaas/api/TMP").then().statusCode(404);
		when().get("/eaas/api/TMP").then().statusCode(404);
	}

	@Test
	public void uploadOfNonExcelIsBadRequest() {
		given().multiPart("file", "x.txt", "not excel".getBytes()).when().post("/eaas/api/BAD").then()
				.statusCode(400);
	}

	@Test
	public void uploadWithoutFileIsBadRequest() {
		given().multiPart("other", "x").when().post("/eaas/api/BAD").then().statusCode(400);
	}
}
