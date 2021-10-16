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
						"results[2].value", is("IF(B2,50,0)"));

	}
	
	
	@Test
	public void testKYCComputeKYCC6() {

		when()
			.get("/api/{resource}/{sheet}/{cell}", "KYC", "ComputeKYC", "C6")
		.then()
			.statusCode(200)
				.body("_count", equalTo(1),
						"results[0].address", is("C6"),
						"results[0].value", is(0.0F),
						"results[0].type", is("NUMERIC"));

	}

}