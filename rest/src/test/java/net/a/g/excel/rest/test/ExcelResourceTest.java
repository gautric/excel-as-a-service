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
	public void testKYCComputeKYCCells() {

		when().get("/eaas/api/{resource}/sheet/{sheet}/cells", "KYC", "ComputeKYC")

				.then().statusCode(200)

				// @formatter:off
			.body(is("{\n"
					+ "  \"_links\" : [ {\n"
					+ "    \"rel\" : \"self\",\n"
					+ "    \"href\" : \"http://localhost:8081/eaas/api/KYC/sheet/ComputeKYC/cells\",\n"
					+ "    \"type\" : \"application/json\"\n"
					+ "  } ],\n"
					+ "  \"_count\" : 15,\n"
					+ "  \"cells\" : [ {\n"
					+ "    \"_links\" : [ {\n"
					+ "      \"rel\" : \"resource\",\n"
					+ "      \"href\" : \"http://localhost:8081/eaas/api/KYC\",\n"
					+ "      \"type\" : \"application/json\"\n"
					+ "    }, {\n"
					+ "      \"rel\" : \"sheet\",\n"
					+ "      \"href\" : \"http://localhost:8081/eaas/api/KYC/sheet/ComputeKYC\",\n"
					+ "      \"type\" : \"application/json\"\n"
					+ "    }, {\n"
					+ "      \"rel\" : \"self\",\n"
					+ "      \"href\" : \"http://localhost:8081/eaas/api/KYC/sheet/ComputeKYC/cell/A1\",\n"
					+ "      \"type\" : \"application/json\"\n"
					+ "    } ],\n"
					+ "    \"address\" : \"ComputeKYC!A1\",\n"
					+ "    \"value\" : \"\",\n"
					+ "    \"type\" : \"BLANK\"\n"
					+ "  }, {\n"
					+ "    \"_links\" : [ {\n"
					+ "      \"rel\" : \"resource\",\n"
					+ "      \"href\" : \"http://localhost:8081/eaas/api/KYC\",\n"
					+ "      \"type\" : \"application/json\"\n"
					+ "    }, {\n"
					+ "      \"rel\" : \"sheet\",\n"
					+ "      \"href\" : \"http://localhost:8081/eaas/api/KYC/sheet/ComputeKYC\",\n"
					+ "      \"type\" : \"application/json\"\n"
					+ "    }, {\n"
					+ "      \"rel\" : \"self\",\n"
					+ "      \"href\" : \"http://localhost:8081/eaas/api/KYC/sheet/ComputeKYC/cell/B1\",\n"
					+ "      \"type\" : \"application/json\"\n"
					+ "    } ],\n"
					+ "    \"address\" : \"ComputeKYC!B1\",\n"
					+ "    \"value\" : \"INPUT\",\n"
					+ "    \"type\" : \"STRING\"\n"
					+ "  }, {\n"
					+ "    \"_links\" : [ {\n"
					+ "      \"rel\" : \"resource\",\n"
					+ "      \"href\" : \"http://localhost:8081/eaas/api/KYC\",\n"
					+ "      \"type\" : \"application/json\"\n"
					+ "    }, {\n"
					+ "      \"rel\" : \"sheet\",\n"
					+ "      \"href\" : \"http://localhost:8081/eaas/api/KYC/sheet/ComputeKYC\",\n"
					+ "      \"type\" : \"application/json\"\n"
					+ "    }, {\n"
					+ "      \"rel\" : \"self\",\n"
					+ "      \"href\" : \"http://localhost:8081/eaas/api/KYC/sheet/ComputeKYC/cell/C1\",\n"
					+ "      \"type\" : \"application/json\"\n"
					+ "    } ],\n"
					+ "    \"address\" : \"ComputeKYC!C1\",\n"
					+ "    \"value\" : \" SCORE\",\n"
					+ "    \"type\" : \"STRING\"\n"
					+ "  }, {\n"
					+ "    \"_links\" : [ {\n"
					+ "      \"rel\" : \"resource\",\n"
					+ "      \"href\" : \"http://localhost:8081/eaas/api/KYC\",\n"
					+ "      \"type\" : \"application/json\"\n"
					+ "    }, {\n"
					+ "      \"rel\" : \"sheet\",\n"
					+ "      \"href\" : \"http://localhost:8081/eaas/api/KYC/sheet/ComputeKYC\",\n"
					+ "      \"type\" : \"application/json\"\n"
					+ "    }, {\n"
					+ "      \"rel\" : \"self\",\n"
					+ "      \"href\" : \"http://localhost:8081/eaas/api/KYC/sheet/ComputeKYC/cell/A2\",\n"
					+ "      \"type\" : \"application/json\"\n"
					+ "    } ],\n"
					+ "    \"address\" : \"ComputeKYC!A2\",\n"
					+ "    \"value\" : \"PEP\",\n"
					+ "    \"type\" : \"STRING\"\n"
					+ "  }, {\n"
					+ "    \"_links\" : [ {\n"
					+ "      \"rel\" : \"resource\",\n"
					+ "      \"href\" : \"http://localhost:8081/eaas/api/KYC\",\n"
					+ "      \"type\" : \"application/json\"\n"
					+ "    }, {\n"
					+ "      \"rel\" : \"sheet\",\n"
					+ "      \"href\" : \"http://localhost:8081/eaas/api/KYC/sheet/ComputeKYC\",\n"
					+ "      \"type\" : \"application/json\"\n"
					+ "    }, {\n"
					+ "      \"rel\" : \"self\",\n"
					+ "      \"href\" : \"http://localhost:8081/eaas/api/KYC/sheet/ComputeKYC/cell/B2\",\n"
					+ "      \"type\" : \"application/json\"\n"
					+ "    } ],\n"
					+ "    \"address\" : \"ComputeKYC!B2\",\n"
					+ "    \"value\" : \"false\",\n"
					+ "    \"metadata\" : \"@input(PEP)\",\n"
					+ "    \"type\" : \"BOOLEAN\"\n"
					+ "  }, {\n"
					+ "    \"_links\" : [ {\n"
					+ "      \"rel\" : \"resource\",\n"
					+ "      \"href\" : \"http://localhost:8081/eaas/api/KYC\",\n"
					+ "      \"type\" : \"application/json\"\n"
					+ "    }, {\n"
					+ "      \"rel\" : \"sheet\",\n"
					+ "      \"href\" : \"http://localhost:8081/eaas/api/KYC/sheet/ComputeKYC\",\n"
					+ "      \"type\" : \"application/json\"\n"
					+ "    }, {\n"
					+ "      \"rel\" : \"self\",\n"
					+ "      \"href\" : \"http://localhost:8081/eaas/api/KYC/sheet/ComputeKYC/cell/C2\",\n"
					+ "      \"type\" : \"application/json\"\n"
					+ "    } ],\n"
					+ "    \"address\" : \"ComputeKYC!C2\",\n"
					+ "    \"value\" : \"IF(B2,50,0)\",\n"
					+ "    \"type\" : \"FORMULA\"\n"
					+ "  }, {\n"
					+ "    \"_links\" : [ {\n"
					+ "      \"rel\" : \"resource\",\n"
					+ "      \"href\" : \"http://localhost:8081/eaas/api/KYC\",\n"
					+ "      \"type\" : \"application/json\"\n"
					+ "    }, {\n"
					+ "      \"rel\" : \"sheet\",\n"
					+ "      \"href\" : \"http://localhost:8081/eaas/api/KYC/sheet/ComputeKYC\",\n"
					+ "      \"type\" : \"application/json\"\n"
					+ "    }, {\n"
					+ "      \"rel\" : \"self\",\n"
					+ "      \"href\" : \"http://localhost:8081/eaas/api/KYC/sheet/ComputeKYC/cell/A3\",\n"
					+ "      \"type\" : \"application/json\"\n"
					+ "    } ],\n"
					+ "    \"address\" : \"ComputeKYC!A3\",\n"
					+ "    \"value\" : \"COUNTRY\",\n"
					+ "    \"type\" : \"STRING\"\n"
					+ "  }, {\n"
					+ "    \"_links\" : [ {\n"
					+ "      \"rel\" : \"resource\",\n"
					+ "      \"href\" : \"http://localhost:8081/eaas/api/KYC\",\n"
					+ "      \"type\" : \"application/json\"\n"
					+ "    }, {\n"
					+ "      \"rel\" : \"sheet\",\n"
					+ "      \"href\" : \"http://localhost:8081/eaas/api/KYC/sheet/ComputeKYC\",\n"
					+ "      \"type\" : \"application/json\"\n"
					+ "    }, {\n"
					+ "      \"rel\" : \"self\",\n"
					+ "      \"href\" : \"http://localhost:8081/eaas/api/KYC/sheet/ComputeKYC/cell/B3\",\n"
					+ "      \"type\" : \"application/json\"\n"
					+ "    } ],\n"
					+ "    \"address\" : \"ComputeKYC!B3\",\n"
					+ "    \"value\" : \"FR\",\n"
					+ "    \"metadata\" : \"@input(COUNTRY)\",\n"
					+ "    \"type\" : \"STRING\"\n"
					+ "  }, {\n"
					+ "    \"_links\" : [ {\n"
					+ "      \"rel\" : \"resource\",\n"
					+ "      \"href\" : \"http://localhost:8081/eaas/api/KYC\",\n"
					+ "      \"type\" : \"application/json\"\n"
					+ "    }, {\n"
					+ "      \"rel\" : \"sheet\",\n"
					+ "      \"href\" : \"http://localhost:8081/eaas/api/KYC/sheet/ComputeKYC\",\n"
					+ "      \"type\" : \"application/json\"\n"
					+ "    }, {\n"
					+ "      \"rel\" : \"self\",\n"
					+ "      \"href\" : \"http://localhost:8081/eaas/api/KYC/sheet/ComputeKYC/cell/C3\",\n"
					+ "      \"type\" : \"application/json\"\n"
					+ "    } ],\n"
					+ "    \"address\" : \"ComputeKYC!C3\",\n"
					+ "    \"value\" : \"VLOOKUP(B3,COUNTRY!A1:B5,2,FALSE)\",\n"
					+ "    \"type\" : \"FORMULA\"\n"
					+ "  }, {\n"
					+ "    \"_links\" : [ {\n"
					+ "      \"rel\" : \"resource\",\n"
					+ "      \"href\" : \"http://localhost:8081/eaas/api/KYC\",\n"
					+ "      \"type\" : \"application/json\"\n"
					+ "    }, {\n"
					+ "      \"rel\" : \"sheet\",\n"
					+ "      \"href\" : \"http://localhost:8081/eaas/api/KYC/sheet/ComputeKYC\",\n"
					+ "      \"type\" : \"application/json\"\n"
					+ "    }, {\n"
					+ "      \"rel\" : \"self\",\n"
					+ "      \"href\" : \"http://localhost:8081/eaas/api/KYC/sheet/ComputeKYC/cell/A4\",\n"
					+ "      \"type\" : \"application/json\"\n"
					+ "    } ],\n"
					+ "    \"address\" : \"ComputeKYC!A4\",\n"
					+ "    \"value\" : \"AMOUNT\",\n"
					+ "    \"type\" : \"STRING\"\n"
					+ "  }, {\n"
					+ "    \"_links\" : [ {\n"
					+ "      \"rel\" : \"resource\",\n"
					+ "      \"href\" : \"http://localhost:8081/eaas/api/KYC\",\n"
					+ "      \"type\" : \"application/json\"\n"
					+ "    }, {\n"
					+ "      \"rel\" : \"sheet\",\n"
					+ "      \"href\" : \"http://localhost:8081/eaas/api/KYC/sheet/ComputeKYC\",\n"
					+ "      \"type\" : \"application/json\"\n"
					+ "    }, {\n"
					+ "      \"rel\" : \"self\",\n"
					+ "      \"href\" : \"http://localhost:8081/eaas/api/KYC/sheet/ComputeKYC/cell/B4\",\n"
					+ "      \"type\" : \"application/json\"\n"
					+ "    } ],\n"
					+ "    \"address\" : \"ComputeKYC!B4\",\n"
					+ "    \"value\" : 0.0,\n"
					+ "    \"metadata\" : \"@input(AMOUNT)\",\n"
					+ "    \"type\" : \"NUMERIC\"\n"
					+ "  }, {\n"
					+ "    \"_links\" : [ {\n"
					+ "      \"rel\" : \"resource\",\n"
					+ "      \"href\" : \"http://localhost:8081/eaas/api/KYC\",\n"
					+ "      \"type\" : \"application/json\"\n"
					+ "    }, {\n"
					+ "      \"rel\" : \"sheet\",\n"
					+ "      \"href\" : \"http://localhost:8081/eaas/api/KYC/sheet/ComputeKYC\",\n"
					+ "      \"type\" : \"application/json\"\n"
					+ "    }, {\n"
					+ "      \"rel\" : \"self\",\n"
					+ "      \"href\" : \"http://localhost:8081/eaas/api/KYC/sheet/ComputeKYC/cell/C4\",\n"
					+ "      \"type\" : \"application/json\"\n"
					+ "    } ],\n"
					+ "    \"address\" : \"ComputeKYC!C4\",\n"
					+ "    \"value\" : \"VLOOKUP(B4,AMOUNT!A1:B5,2,TRUE)\",\n"
					+ "    \"type\" : \"FORMULA\"\n"
					+ "  }, {\n"
					+ "    \"_links\" : [ {\n"
					+ "      \"rel\" : \"resource\",\n"
					+ "      \"href\" : \"http://localhost:8081/eaas/api/KYC\",\n"
					+ "      \"type\" : \"application/json\"\n"
					+ "    }, {\n"
					+ "      \"rel\" : \"sheet\",\n"
					+ "      \"href\" : \"http://localhost:8081/eaas/api/KYC/sheet/ComputeKYC\",\n"
					+ "      \"type\" : \"application/json\"\n"
					+ "    }, {\n"
					+ "      \"rel\" : \"self\",\n"
					+ "      \"href\" : \"http://localhost:8081/eaas/api/KYC/sheet/ComputeKYC/cell/A5\",\n"
					+ "      \"type\" : \"application/json\"\n"
					+ "    } ],\n"
					+ "    \"address\" : \"ComputeKYC!A5\",\n"
					+ "    \"value\" : \"\",\n"
					+ "    \"type\" : \"BLANK\"\n"
					+ "  }, {\n"
					+ "    \"_links\" : [ {\n"
					+ "      \"rel\" : \"resource\",\n"
					+ "      \"href\" : \"http://localhost:8081/eaas/api/KYC\",\n"
					+ "      \"type\" : \"application/json\"\n"
					+ "    }, {\n"
					+ "      \"rel\" : \"sheet\",\n"
					+ "      \"href\" : \"http://localhost:8081/eaas/api/KYC/sheet/ComputeKYC\",\n"
					+ "      \"type\" : \"application/json\"\n"
					+ "    }, {\n"
					+ "      \"rel\" : \"self\",\n"
					+ "      \"href\" : \"http://localhost:8081/eaas/api/KYC/sheet/ComputeKYC/cell/A6\",\n"
					+ "      \"type\" : \"application/json\"\n"
					+ "    } ],\n"
					+ "    \"address\" : \"ComputeKYC!A6\",\n"
					+ "    \"value\" : \"FINAL\",\n"
					+ "    \"type\" : \"STRING\"\n"
					+ "  }, {\n"
					+ "    \"_links\" : [ {\n"
					+ "      \"rel\" : \"resource\",\n"
					+ "      \"href\" : \"http://localhost:8081/eaas/api/KYC\",\n"
					+ "      \"type\" : \"application/json\"\n"
					+ "    }, {\n"
					+ "      \"rel\" : \"sheet\",\n"
					+ "      \"href\" : \"http://localhost:8081/eaas/api/KYC/sheet/ComputeKYC\",\n"
					+ "      \"type\" : \"application/json\"\n"
					+ "    }, {\n"
					+ "      \"rel\" : \"self\",\n"
					+ "      \"href\" : \"http://localhost:8081/eaas/api/KYC/sheet/ComputeKYC/cell/C6\",\n"
					+ "      \"type\" : \"application/json\"\n"
					+ "    } ],\n"
					+ "    \"address\" : \"ComputeKYC!C6\",\n"
					+ "    \"value\" : \"SUM(C2:C4)\",\n"
					+ "    \"metadata\" : \"@output(SCORE)\",\n"
					+ "    \"type\" : \"FORMULA\"\n"
					+ "  } ]\n"
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
					+ "  \"metadata\" : \"@output(SCORE)\",\n"
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
					+ "  \"metadata\" : \"@output(SCORE)\",\n"
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
					+ "  \"metadata\" : \"@output(SCORE)\",\n"
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
					+ "  \"metadata\" : \"@output(SCORE)\",\n"
					+ "  \"type\" : \"NUMERIC\"\n"
					+ "}"));
	// @formatter:on

	}
	
	@Test
	public void testKYCComputeKYCAPI() {

		when().get("/eaas/api/KYC/sheet/ComputeKYC/compute/SCORE/PEP/true/COUNTRY/CY/AMOUNT/1000000").then().statusCode(200)
	// @formatter:off
			.body(is("{\n"
					+ "  \"_links\" : [ {\n"
					+ "    \"rel\" : \"self\",\n"
					+ "    \"href\" : \"http://localhost:8081/eaas/api/KYC/sheet/ComputeKYC/compute/SCORE/PEP/true/COUNTRY/CY/AMOUNT/1000000\",\n"
					+ "    \"type\" : \"application/json\"\n"
					+ "  } ],\n"
					+ "  \"_count\" : 1,\n"
					+ "  \"cells\" : [ {\n"
					+ "    \"_links\" : [ {\n"
					+ "      \"rel\" : \"template\",\n"
					+ "      \"href\" : \"http://localhost:8081/eaas/api/KYC/sheet/ComputeKYC/compute/SCORE/PEP/{PEP: true|false}/COUNTRY/{COUNTRY: [^/]+}/AMOUNT/{AMOUNT: [+-]?([0-9]*).?[0-9]+}\",\n"
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
	public void testKYCComputeKYCAPI_KO() {

		when().get("/eaas/api/KYC/sheet/ComputeKYC/compute/SCORE/PEP/true/COUNTRY/CY/AMOUNT").then().statusCode(400);
	// @formatter:off
			
	// @formatter:on

	}

	
	@Test
	public void testKYCComputeKYCAPI_NO() {

		when().get("/eaas/api/KYC/sheet/ComputeKYC/compute/SCORE").then().statusCode(500);
	// @formatter:off
		
	// @formatter:on

	}
}

