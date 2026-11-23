package net.a.g.excel.lambda;

import static net.a.g.excel.lambda.LambdaFixture.inputs;
import static net.a.g.excel.lambda.LambdaFixture.request;
import static net.a.g.excel.lambda.LambdaFixture.value;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import net.a.g.excel.model.ExcelRequest;
import net.a.g.excel.model.ExcelResult;

/**
 * Primary!Feuil1: A1 = [Secondary.xlsx]Feuil1!A1+B1+C1 (40+1+1 = 42), B1 = A1+2+C1, C1 = -2.
 */
class GlobalModeTest {

	private static ExcelLambdaHandler handler;

	@BeforeAll
	static void setup() {
		handler = LambdaFixture.handler("Primary", "Secondary", "KYCAPI");
	}

	private static ExcelResult global(String uuid, List<String> outputs, Map<String, String> in) {
		ExcelRequest req = request(uuid, "Primary", "Feuil1", outputs, in);
		req.setGlobal(true);
		return handler.handleRequest(req, null);
	}

	@ParameterizedTest(name = "C1={0} -> A1={1}, B1={2}")
	@CsvSource({ "0,42,44", "10,42,54", "2.5,42,46.5", "1000,42,1044" })
	void crossWorkbookReferenceResolves(String c1, double a1, double b1) {
		ExcelResult res = global("g-" + c1, List.of("A1", "B1"), inputs("C1", c1));
		assertNull(res.getError(), () -> res.getError().getError());
		assertEquals(a1, value(res, "Feuil1!A1"));
		assertEquals(b1, value(res, "Feuil1!B1"));
	}

	@Test
	void globalResultKeepsUuidAndCount() {
		ExcelResult res = global("corr", List.of("A1", "B1", "C1"), inputs("C1", "0"));
		assertEquals("corr", res.getUuid());
		assertEquals(3, res.getCount());
	}

	@Test
	void secondaryIsComputedStandaloneWithoutGlobal() {
		ExcelResult res = handler.handleRequest(
				request("sec", "Secondary", "Feuil1", List.of("A1", "B1", "C1"), inputs("A1", "100")), null);
		assertNull(res.getError());
		assertEquals(100.0, value(res, "Feuil1!A1"));
		assertEquals(1.0, value(res, "Feuil1!C1"));
	}

	@Test
	void globalFalseCannotResolveExternalReference() {
		ExcelRequest req = request("local", "Primary", "Feuil1", List.of("A1"), inputs("C1", "0"));
		req.setGlobal(false);
		ExcelResult res = handler.handleRequest(req, null);
		assertEquals("500", res.getError().getCode());
		assertEquals("local", res.getUuid());
	}

	@Test
	void globalFailsWhenReferencedWorkbookIsMissing() {
		ExcelRequest req = request("orphan", "Primary", "Feuil1", List.of("A1"), inputs("C1", "0"));
		req.setGlobal(true);
		ExcelResult res = LambdaFixture.handler("Primary").handleRequest(req, null);
		assertEquals("500", res.getError().getCode());
		assertEquals("orphan", res.getUuid());
	}

	@Test
	void globalIsHarmlessForSelfContainedWorkbook() {
		ExcelRequest req = request("kyc", "KYCAPI", "ComputeKYC", List.of("SCORE"),
				inputs("PEP", "true", "COUNTRY", "CY", "AMOUNT", "1000000"));
		req.setGlobal(true);
		ExcelResult res = handler.handleRequest(req, null);
		assertNull(res.getError());
		assertEquals(125.0, LambdaFixture.single(res));
	}

	@Test
	void globalWithoutInputsResolves() {
		ExcelResult res = global("noin", List.of("A1"), Map.of());
		assertNull(res.getError());
		assertEquals(42.0, value(res, "Feuil1!A1"));
	}
}
