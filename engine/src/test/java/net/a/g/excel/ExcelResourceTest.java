package net.a.g.excel;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(ExcelProfileTest.class)
public class ExcelResourceTest {

	@Test
	public void testContextRoot() {
		given().when().get("/api").then().statusCode(200).body(is(
				"{\"_count\":1,\"_self\":\"http://localhost:8081/api\",\"results\":[{\"name\":\"KYC\",\"_ref\":\"http://localhost:8081/api/KYC\"}]}"));
	}

	@Test
	public void testKYC() {
		given().when().get("/api/KYC").then().statusCode(200).body(is(
				"{\"_count\":3,\"_self\":\"http://localhost:8081/api/KYC\",\"results\":[{\"name\":\"ComputeKYC\",\"_ref\":\"http://localhost:8081/api/KYC/ComputeKYC\"},{\"name\":\"COUNTRY\",\"_ref\":\"http://localhost:8081/api/KYC/COUNTRY\"},{\"name\":\"AMOUNT\",\"_ref\":\"http://localhost:8081/api/KYC/AMOUNT\"}]}"));
	}

	@Test
	public void testKYCComputeKYC() {
		given().when().get("/api/KYC/ComputeKYC").then().statusCode(200).body(is(
				"{\"_count\":4,\"_self\":\"http://localhost:8081/api/KYC/ComputeKYC\",\"results\":[{\"address\":\"C3\",\"formula\":\"VLOOKUP(B3,COUNTRY!A1:B5,2,FALSE)\",\"_ref\":\"http://localhost:8081/api/KYC/ComputeKYC/C3\"},{\"address\":\"C4\",\"formula\":\"VLOOKUP(B4,AMOUNT!A1:B5,2,TRUE)\",\"_ref\":\"http://localhost:8081/api/KYC/ComputeKYC/C4\"},{\"address\":\"C6\",\"formula\":\"SUM(C2:C4)\",\"_ref\":\"http://localhost:8081/api/KYC/ComputeKYC/C6\"},{\"address\":\"C2\",\"formula\":\"IF(B2,50,0)\",\"_ref\":\"http://localhost:8081/api/KYC/ComputeKYC/C2\"}]}"));
	}

}