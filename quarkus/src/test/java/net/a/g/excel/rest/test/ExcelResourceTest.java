package net.a.g.excel.rest.test;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ExcelResourceTest {

	@Test
	public void testContextRoot() {

		when()
			.get("/api")
		.then()
			.statusCode(200)
				.body("_count", equalTo(1),
						"results[0].name", is("KYC"),
						"results[0]._ref", is("http://localhost:8081/api/KYC"));
		
	}

	@Test
	public void testKYC() {
		
		when()
			.get("/api/{resource}","KYC")
		.then()
			.statusCode(200)
				.body("_count", equalTo(3),
						"results[0].name", is("ComputeKYC"),
						"results[0]._ref", is("http://localhost:8081/api/KYC/ComputeKYC"),
						"results[1].name", is("COUNTRY"),
						"results[1]._ref", is("http://localhost:8081/api/KYC/COUNTRY"));
		
		}

	
	@Test
	public void testKYCComputeKYC2() {

		when()
			.get("/api/{resource}/{sheet}", "KYC", "ComputeKYC")
		.then()
			.statusCode(200)
				.body("_count", equalTo(15),
						"results[2].address", is("C2"),
						"results[2].formula", is("IF(B2,50,0)"));

	}

}