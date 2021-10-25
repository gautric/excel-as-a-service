package net.a.g.excel.rest.test;

import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ExcelResourceTest {

	@Test
	public void testContextRoot() {

//		when()
//			.get("/eaas/api")
//		.then()
//			.statusCode(200)
//				.body(is("{}"));
//		
		when().get("/eaas/api")

				.then().statusCode(200)

				// @formatter:off
			.body("_count", equalTo(1), 
				"resources[0].name", is("KYC"),
				"resources[0].file", is("KYC.xlsx"),
				"resources[0]._links[0].href", is("http://localhost:8081/eaas/api"), 
				"resources[0]._links[0].rel", is("list-of-resource"),
				"resources[0]._links[1].href", is("http://localhost:8081/eaas/api/KYC"), 
				"resources[0]._links[1].rel", is("self"), 
				"resources[0]._links[2].href", is("http://localhost:8081/eaas/api/KYC/download"),
				"resources[0]._links[3].href", is("http://localhost:8081/eaas/api/KYC/sheets"),
				"resources[0]._links[3].rel", is("list-of-sheet")
			// @formatter:on
				);

	}

	@Test
	public void testKYC() {

		when().get("/eaas/api/KYC").then().statusCode(200)

		// @formatter:off
			.body(is("{\n"
					+ "  \"_links\" : [ {\n"
					+ "    \"rel\" : \"list-of-resource\",\n"
					+ "    \"href\" : \"http://localhost:8081/eaas/api\",\n"
					+ "    \"type\" : \"application/json\"\n"
					+ "  }, {\n"
					+ "    \"rel\" : \"self\",\n"
					+ "    \"href\" : \"http://localhost:8081/eaas/api/KYC\",\n"
					+ "    \"type\" : \"application/json\"\n"
					+ "  }, {\n"
					+ "    \"rel\" : \"download\",\n"
					+ "    \"href\" : \"http://localhost:8081/eaas/api/KYC/download\",\n"
					+ "    \"type\" : \"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\"\n"
					+ "  }, {\n"
					+ "    \"rel\" : \"list-of-sheet\",\n"
					+ "    \"href\" : \"http://localhost:8081/eaas/api/KYC/sheets\",\n"
					+ "    \"type\" : \"application/json\"\n"
					+ "  } ],\n"
					+ "  \"name\" : \"KYC\",\n"
					+ "  \"file\" : \"KYC.xlsx\"\n"
					+ "}"));
		// @formatter:on

	}

	@Test
	public void testKYCComputeKYC2() {

		when().get("/eaas/api/{resource}/sheet/{sheet}", "KYC", "ComputeKYC")

				.then().statusCode(200)

				// @formatter:off
			.body(is("{\n"
					+ "  \"_links\" : [ {\n"
					+ "    \"rel\" : \"resource\",\n"
					+ "    \"href\" : \"http://localhost:8081/eaas/api/KYC\",\n"
					+ "    \"type\" : \"application/json\"\n"
					+ "  }, {\n"
					+ "    \"rel\" : \"self\",\n"
					+ "    \"href\" : \"http://localhost:8081/eaas/api/KYC/sheet/ComputeKYC\",\n"
					+ "    \"type\" : \"application/json\"\n"
					+ "  }, {\n"
					+ "    \"rel\" : \"list-of-cell\",\n"
					+ "    \"href\" : \"http://localhost:8081/eaas/api/KYC/sheet/ComputeKYC/cells\",\n"
					+ "    \"type\" : \"application/json\"\n"
					+ "  } ],\n"
					+ "  \"name\" : \"ComputeKYC\"\n"
					+ "}"));
		// @formatter:on

	}

	@Test
	public void testKYCComputeKYC_404() {

		when().get("/eaas/api/{resource}/sheet/{sheet}/cell/{cell}", "KYC", "ComputeKYC", "CPPL").then()
			.statusCode(404);
	}

	@Test
	public void testKYCComputeKYCC6() {

		when().get("/eaas/api/{resource}/sheet/{sheet}/cell/{cell}", "KYC", "ComputeKYC", "C6").then().statusCode(200)
		// @formatter:off

			.body(is("{\n"
					+ "  \"_links\" : [ {\n"
					+ "    \"rel\" : \"resource\",\n"
					+ "    \"href\" : \"http://localhost:8081/eaas/api/KYC\",\n"
					+ "    \"type\" : \"application/json\"\n"
					+ "  }, {\n"
					+ "    \"rel\" : \"sheet\",\n"
					+ "    \"href\" : \"http://localhost:8081/eaas/api/KYC/sheet/ComputeKYC\",\n"
					+ "    \"type\" : \"application/json\"\n"
					+ "  }, {\n"
					+ "    \"rel\" : \"self\",\n"
					+ "    \"href\" : \"http://localhost:8081/eaas/api/KYC/sheet/ComputeKYC/cell/C6\",\n"
					+ "    \"type\" : \"application/json\"\n"
					+ "  } ],\n"
					+ "  \"address\" : \"ComputeKYC!C6\",\n"
					+ "  \"value\" : \"SUM(C2:C4)\",\n"
					+ "  \"metadata\" : \"@output\",\n"
					+ "  \"type\" : \"FORMULA\"\n"
					+ "}"));
		// @formatter:on

	}

	@Test
	public void testKYCComputeKYCC6PEP() {

		when().get("/eaas/api/{resource}/sheet/{sheet}/cell/{cell}?{input}={value}", "KYC", "ComputeKYC", "C6", "B2",
				"TRUE").then().statusCode(200)
	// @formatter:off

			.body(is("{\n"
					+ "  \"_links\" : [ {\n"
					+ "    \"rel\" : \"resource\",\n"
					+ "    \"href\" : \"http://localhost:8081/eaas/api/KYC\",\n"
					+ "    \"type\" : \"application/json\"\n"
					+ "  }, {\n"
					+ "    \"rel\" : \"sheet\",\n"
					+ "    \"href\" : \"http://localhost:8081/eaas/api/KYC/sheet/ComputeKYC\",\n"
					+ "    \"type\" : \"application/json\"\n"
					+ "  }, {\n"
					+ "    \"rel\" : \"self\",\n"
					+ "    \"href\" : \"http://localhost:8081/eaas/api/KYC/sheet/ComputeKYC/cell/C6\",\n"
					+ "    \"type\" : \"application/json\"\n"
					+ "  }, {\n"
					+ "    \"rel\" : \"query\",\n"
					+ "    \"href\" : \"http://localhost:8081/eaas/api/KYC/sheet/ComputeKYC/cell/C6?B2=TRUE\",\n"
					+ "    \"type\" : \"application/json\"\n"
					+ "  } ],\n"
					+ "  \"address\" : \"ComputeKYC!C6\",\n"
					+ "  \"value\" : 50.0,\n"
					+ "  \"metadata\" : \"@output\",\n"
					+ "  \"type\" : \"NUMERIC\"\n"
					+ "}"));
	// @formatter:on

	}

	@Test
	public void testKYCComputeKYCCY() {

		when().get("/eaas/api/{resource}/sheet/{sheet}/cell/{cell}?{input}={value}", "KYC", "ComputeKYC", "C6", "B3",
				"CY").then().statusCode(200)
	// @formatter:off

			.body(is("{\n"
					+ "  \"_links\" : [ {\n"
					+ "    \"rel\" : \"resource\",\n"
					+ "    \"href\" : \"http://localhost:8081/eaas/api/KYC\",\n"
					+ "    \"type\" : \"application/json\"\n"
					+ "  }, {\n"
					+ "    \"rel\" : \"sheet\",\n"
					+ "    \"href\" : \"http://localhost:8081/eaas/api/KYC/sheet/ComputeKYC\",\n"
					+ "    \"type\" : \"application/json\"\n"
					+ "  }, {\n"
					+ "    \"rel\" : \"self\",\n"
					+ "    \"href\" : \"http://localhost:8081/eaas/api/KYC/sheet/ComputeKYC/cell/C6\",\n"
					+ "    \"type\" : \"application/json\"\n"
					+ "  }, {\n"
					+ "    \"rel\" : \"query\",\n"
					+ "    \"href\" : \"http://localhost:8081/eaas/api/KYC/sheet/ComputeKYC/cell/C6?B3=CY\",\n"
					+ "    \"type\" : \"application/json\"\n"
					+ "  } ],\n"
					+ "  \"address\" : \"ComputeKYC!C6\",\n"
					+ "  \"value\" : 25.0,\n"
					+ "  \"metadata\" : \"@output\",\n"
					+ "  \"type\" : \"NUMERIC\"\n"
					+ "}"));
	// @formatter:on

	}

	@Test
	public void testKYCComputeKYCAmount() {

		when().get("/eaas/api/{resource}/sheet/{sheet}/cell/{cell}?{input}={value}", "KYC", "ComputeKYC", "C6", "B4",
				"10000000").then().statusCode(200)
	// @formatter:off

			.body(is("{\n"
					+ "  \"_links\" : [ {\n"
					+ "    \"rel\" : \"resource\",\n"
					+ "    \"href\" : \"http://localhost:8081/eaas/api/KYC\",\n"
					+ "    \"type\" : \"application/json\"\n"
					+ "  }, {\n"
					+ "    \"rel\" : \"sheet\",\n"
					+ "    \"href\" : \"http://localhost:8081/eaas/api/KYC/sheet/ComputeKYC\",\n"
					+ "    \"type\" : \"application/json\"\n"
					+ "  }, {\n"
					+ "    \"rel\" : \"self\",\n"
					+ "    \"href\" : \"http://localhost:8081/eaas/api/KYC/sheet/ComputeKYC/cell/C6\",\n"
					+ "    \"type\" : \"application/json\"\n"
					+ "  }, {\n"
					+ "    \"rel\" : \"query\",\n"
					+ "    \"href\" : \"http://localhost:8081/eaas/api/KYC/sheet/ComputeKYC/cell/C6?B4=10000000\",\n"
					+ "    \"type\" : \"application/json\"\n"
					+ "  } ],\n"
					+ "  \"address\" : \"ComputeKYC!C6\",\n"
					+ "  \"value\" : 75.0,\n"
					+ "  \"metadata\" : \"@output\",\n"
					+ "  \"type\" : \"NUMERIC\"\n"
					+ "}"));
	// @formatter:on

	}

}
