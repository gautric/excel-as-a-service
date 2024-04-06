package net.a.g.excel.rest.test;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.is;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import net.a.g.excel.model.ExcelRequest;



@QuarkusTest
@TestProfile(Profile2.class)
public class ExcelResourceRESTTest_w_Profile2 {
	
	@Test
	public void test_ok_KYC_ComputeKYC_COMPUTE_SCORE_PEP_true_COUNTRY_CY_AMOUNT_1M() {

		when().get("/eaas/api/KYCAPI/sheet/ComputeKYC/compute/SCORE/PEP/true/COUNTRY/CY/AMOUNT/1000000").then()
				.statusCode(200)
				// @formatter:off
			.body(is("{\n"
					+ "  \"_links\" : [ {\n"
					+ "    \"rel\" : \"sheet\",\n"
					+ "    \"href\" : \"http://localhost:8081/eaas/api/KYCAPI/sheet/ComputeKYC\",\n"
					+ "    \"type\" : \"application/json\"\n"
					+ "  }, {\n"
					+ "    \"rel\" : \"self\",\n"
					+ "    \"href\" : \"http://localhost:8081/eaas/api/KYCAPI/sheet/ComputeKYC/compute/SCORE/PEP/true/COUNTRY/CY/AMOUNT/1000000\",\n"
					+ "    \"type\" : \"application/json\"\n"
					+ "  } ],\n"
					+ "  \"_count\" : 1,\n"
					+ "  \"cells\" : [ {\n"
					+ "    \"_links\" : [ {\n"
					+ "      \"rel\" : \"uri-template\",\n"
					+ "      \"href\" : \"http://localhost:8081/eaas/api/KYCAPI/sheet/ComputeKYC/compute/SCORE/PEP/{PEP}/COUNTRY/{COUNTRY}/AMOUNT/{AMOUNT}\",\n"
					+ "      \"type\" : \"application/json\"\n"
					+ "    } ],\n"
					+ "    \"address\" : \"ComputeKYC!C6\",\n"
					+ "    \"value\" : 125.0,\n"
					+ "    \"metadata\" : \"@output(SCORE)\",\n"
					+ "    \"type\" : \"NUMERIC\"\n"
					+ "  } ]\n"
					+ "}"));
	// @formatter:on

	}

	@Test
	public void test_ok_KYC_ComputeKYC_SCORE_PEP_true_COUNTRY_CY_TOTOTO_1000000() {

		when().get("/eaas/api/KYCAPI/sheet/ComputeKYC/compute/SCORE/PEP/true/COUNTRY/CY/TOTOTO/1000000").then()
				.statusCode(200)
				// @formatter:off
			.body(is("{\n"
					+ "  \"_links\" : [ {\n"
					+ "    \"rel\" : \"sheet\",\n"
					+ "    \"href\" : \"http://localhost:8081/eaas/api/KYCAPI/sheet/ComputeKYC\",\n"
					+ "    \"type\" : \"application/json\"\n"
					+ "  }, {\n"
					+ "    \"rel\" : \"self\",\n"
					+ "    \"href\" : \"http://localhost:8081/eaas/api/KYCAPI/sheet/ComputeKYC/compute/SCORE/PEP/true/COUNTRY/CY/TOTOTO/1000000\",\n"
					+ "    \"type\" : \"application/json\"\n"
					+ "  } ],\n"
					+ "  \"_count\" : 1,\n"
					+ "  \"cells\" : [ {\n"
					+ "    \"_links\" : [ {\n"
					+ "      \"rel\" : \"uri-template\",\n"
					+ "      \"href\" : \"http://localhost:8081/eaas/api/KYCAPI/sheet/ComputeKYC/compute/SCORE/PEP/{PEP}/COUNTRY/{COUNTRY}/AMOUNT/{AMOUNT}\",\n"
					+ "      \"type\" : \"application/json\"\n"
					+ "    } ],\n"
					+ "    \"address\" : \"ComputeKYC!C6\",\n"
					+ "    \"value\" : 75.0,\n"
					+ "    \"metadata\" : \"@output(SCORE)\",\n"
					+ "    \"type\" : \"NUMERIC\"\n"
					+ "  } ]\n"
					+ "}"));
	// @formatter:on

	}

	@Test
	@Disabled
	public void test_ok_POST_KYC_ComputeKYC_SCORE_PEP_true_COUNTRY_CY_TOTOTO_1000000() throws JsonProcessingException {

		JSONObject body = new JSONObject();
		body.put("resource", "KYC");
		body.put("sheet", "ComputeKYC");
		body.put("force", true);
		List a = new ArrayList();
		a.add("SCORE");
		body.put("outputs", a);

		ExcelRequest er = new ExcelRequest();
		er.setForce(true);
		er.setResource("KYC");
		er.setSheet("ComputeKYC");
		er.getOutputs().add("SCORE");
		
		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString(er);
		System.out.println("ResultingJSONstring = " + json);
		
		given().contentType(ContentType.JSON).body(json).when().post("/eaas/api/compute") // Replace with your
																							// actual endpoint
				.then().statusCode(200).body(is("")); // R

		when().post("/eaas/api/compute").then().statusCode(200).contentType(ContentType.JSON)
	// @formatter:off
			.body(is("{\n"
					+ "  \"_links\" : [ {\n"
					+ "    \"rel\" : \"sheet\",\n"
					+ "    \"href\" : \"http://localhost:8081/eaas/api/KYC/sheet/ComputeKYC\",\n"
					+ "    \"type\" : \"application/json\"\n"
					+ "  }, {\n"
					+ "    \"rel\" : \"self\",\n"
					+ "    \"href\" : \"http://localhost:8081/eaas/api/KYC/sheet/ComputeKYC/compute/SCORE/PEP/true/COUNTRY/CY/TOTOTO/1000000\",\n"
					+ "    \"type\" : \"application/json\"\n"
					+ "  } ],\n"
					+ "  \"_count\" : 1,\n"
					+ "  \"cells\" : [ {\n"
					+ "    \"_links\" : [ {\n"
					+ "      \"rel\" : \"uri-template\",\n"
					+ "      \"href\" : \"http://localhost:8081/eaas/api/KYC/sheet/ComputeKYC/compute/SCORE/PEP/{PEP}/COUNTRY/{COUNTRY}/AMOUNT/{AMOUNT}\",\n"
					+ "      \"type\" : \"application/json\"\n"
					+ "    } ],\n"
					+ "    \"address\" : \"ComputeKYC!C6\",\n"
					+ "    \"value\" : 75.0,\n"
					+ "    \"metadata\" : \"@output(SCORE)\",\n"
					+ "    \"type\" : \"NUMERIC\"\n"
					+ "  } ]\n"
					+ "}"));
	// @formatter:on

	}
}