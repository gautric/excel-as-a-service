package net.a.g.excel.rest.test;

import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ExcelResourceTest {

	@Test
	public void testContextRoot() {

//		when()
//			.get("/api")
//		.then()
//			.statusCode(200)
//				.body(is("{}"));
		
		when()
			.get("/eaas/api")
		.then()
			.statusCode(200)
				.body("_count", equalTo(1),
						"results[0].name", is("KYC"),
						"results[0].links[0].href", is("http://localhost:8081/eaas/KYC"));
		
	}

	@Test
	public void testKYC() {
		
		when()
			.get("/eaas/api/{resource}","KYC")
		.then()
			.statusCode(200)
				.body("_count", equalTo(3),
						"results[0].name", is("ComputeKYC"),
						"results[0].links[0].href", is("http://localhost:8081/eaas/KYC/ComputeKYC"),
						"results[1].name", is("COUNTRY"),
						"results[1].links[0].href", is("http://localhost:8081/eaas/KYC/COUNTRY"));
		
		}

	
	@Test
	public void testKYCComputeKYC2() {

		when()
			.get("/eaas/api/{resource}/{sheet}", "KYC", "ComputeKYC")
		.then()
			.statusCode(200)
				.body("_count", equalTo(15),
						"results[5].address", is("ComputeKYC!C2"),
						"results[5].value", is("IF(B2,50,0)"));

	}
	
	
	@Test
	public void testKYCComputeKYCC6() {

		when()
			.get("/eaas/api/{resource}/{sheet}/{cell}", "KYC", "ComputeKYC", "C6")
		.then()
			.statusCode(200)
				.body("_count", equalTo(1),
						"results[0].address", is("ComputeKYC!C6"),
						"results[0].value", is(0.0F),
						"results[0].type", is("NUMERIC"));

	}
	
	@Test
	public void testKYCComputeKYCC6PEP() {

		when()
			.get("/eaas/api/{resource}/{sheet}/{cell}?{input}={value}", "KYC", "ComputeKYC", "C6","B2", "TRUE")
		.then()
			.statusCode(200)
				.body("_count", equalTo(1),
						"results[0].address", is("ComputeKYC!C6"),
						"results[0].value", is(50.0F),
						"results[0].type", is("NUMERIC"));

	}
	
	@Test
	public void testKYCComputeKYCCY() {

		when()
			.get("/eaas/api/{resource}/{sheet}/{cell}?{input}={value}", "KYC", "ComputeKYC", "C6","B3", "CY")
		.then()
			.statusCode(200)
				.body("_count", equalTo(1),
						"results[0].address", is("ComputeKYC!C6"),
						"results[0].value", is(25.0F),
						"results[0].type", is("NUMERIC"));

	}
	
	@Test
	public void testKYCComputeKYCAmount() {

		when()
			.get("/eaas/api/{resource}/{sheet}/{cell}?{input}={value}", "KYC", "ComputeKYC", "C6","B4", "10000000")
		.then()
			.statusCode(200)
				.body("_count", equalTo(1),
						"results[0].address", is("ComputeKYC!C6"),
						"results[0].value", is(75.0F),
						"results[0].type", is("NUMERIC"));

	}

}