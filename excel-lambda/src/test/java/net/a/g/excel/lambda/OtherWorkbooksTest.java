package net.a.g.excel.lambda;

import static net.a.g.excel.lambda.LambdaFixture.cells;
import static net.a.g.excel.lambda.LambdaFixture.inputs;
import static net.a.g.excel.lambda.LambdaFixture.request;
import static net.a.g.excel.lambda.LambdaFixture.single;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import net.a.g.excel.model.ExcelResult;

/**
 * Test1 (multi-sheet formulas, no @input/@output), Type (cell types) and KYC (comments without API names).
 */
class OtherWorkbooksTest {

	private static ExcelLambdaHandler handler;

	@BeforeAll
	static void setup() {
		handler = LambdaFixture.handler("Test1", "Type", "KYC", "KYCAPI");
	}

	private static ExcelResult run(String resource, String sheet, List<String> outputs, Map<String, String> in) {
		ExcelResult res = handler.handleRequest(request("w", resource, sheet, outputs, in), null);
		assertNull(res.getError(), () -> res.getError().getError());
		return res;
	}

	// ---- Test1: Sheet2!A1 = Sheet1!A1*Sheet1!A3+Sheet1!A2, Sheet2!B1 = B1&" "&B2, Sheet3!A1 = SUM(A1:A3)

	@ParameterizedTest(name = "Test1 A1={0} A2={1} A3={2} -> {3}")
	@CsvSource({ ",,,42", "5,,,52", ",3,,43", ",,0,2", "1.5,0.5,2,3.5", "0,0,0,0" })
	void test1ArithmeticAcrossSheets(String a1, String a2, String a3, double expected) {
		Map<String, String> in = new java.util.LinkedHashMap<>();
		if (a1 != null) in.put("Sheet1!A1", a1);
		if (a2 != null) in.put("Sheet1!A2", a2);
		if (a3 != null) in.put("Sheet1!A3", a3);
		assertEquals(expected, single(run("Test1", "Sheet2", List.of("A1"), in)));
	}

	@ParameterizedTest(name = "Test1 concat [{0}] [{1}] -> [{2}]")
	@CsvSource({ "Hello,World,Hello World", "Bonjour,,Bonjour World", "Good,Bye,Good Bye", "'',Only,' Only'" })
	void test1StringConcatenation(String b1, String b2, String expected) {
		Map<String, String> in = new java.util.LinkedHashMap<>();
		in.put("Sheet1!B1", b1);
		if (b2 != null) in.put("Sheet1!B2", b2);
		assertEquals(expected, single(run("Test1", "Sheet2", List.of("B1"), in)));
	}

	@Test
	void test1OutputsFromSeveralSheetsInOneRequest() {
		ExcelResult res = run("Test1", null, List.of("Sheet2!A1", "Sheet3!A1", "Sheet2!B1"), inputs("Sheet1!A2", "7"));
		assertEquals(List.of(47.0, 21.0, "Hello World"), cells(res).stream().map(c -> c.getValue()).toList());
		assertEquals("STRING", cells(res).get(2).getType());
	}

	@Test
	void test1FormulasAreEvaluatedEvenWithoutInputs() {
		ExcelResult res = run("Test1", "Sheet3", List.of("A1"), Map.of());
		assertEquals(16.0, single(res));
		assertEquals("NUMERIC", cells(res).get(0).getType());
	}

	// ---- Type: A1 0, A3 'A', C3 blank, A5 date 2021-01-01, A6 123, A7 'Hello ', A8 =A1:A2 (#VALUE), A9 =A1+A2+41

	@ParameterizedTest(name = "Type!{0} -> [{1}] {2}")
	@CsvSource({ "A3,A,STRING", "A7,'Hello ',STRING", "A5,2021-01-01,DATE", "A8,#ERROR,ERROR", "C3,'',BLANK",
			"B1,--,STRING" })
	void typeCellsReadAsIs(String ref, String value, String type) {
		ExcelResult res = run("Type", "Sheet1", List.of(ref), Map.of());
		assertEquals(value, single(res));
		assertEquals(type, cells(res).get(0).getType());
	}

	@ParameterizedTest(name = "Type numeric {0} -> {1}")
	@CsvSource({ "A1,0", "A2,1", "A6,123", "A9,42" })
	void typeNumericCells(String ref, double value) {
		ExcelResult res = run("Type", "Sheet1", List.of(ref), Map.of());
		assertEquals(value, single(res));
		assertEquals("NUMERIC", cells(res).get(0).getType());
	}

	@ParameterizedTest(name = "Type!{0} <- [{1}] reads back [{2}]")
	@CsvSource({ "A3,Z,Z", "A5,2024-02-29,2024-02-29", "A5,1999-12-31,1999-12-31", "C3,filled,filled",
			"A6,0.25,0.25" })
	void typeInputsRoundTrip(String ref, String in, String expected) {
		Object v = single(run("Type", "Sheet1", List.of(ref), inputs(ref, in)));
		assertEquals(expected, String.valueOf(v));
	}

	@ParameterizedTest(name = "Type A1={0}, A2={1} -> A9={2}")
	@CsvSource({ "10,1,52", "0,0,41", "1,1,43" })
	void typeFormulaRecomputed(String a1, String a2, double a9) {
		assertEquals(a9, single(run("Type", "Sheet1", List.of("A9"), inputs("A1", a1, "A2", a2))));
	}

	@ParameterizedTest(name = "Type bad date [{0}] -> 400")
	@CsvSource({ "01/02/2021", "tomorrow", "''" })
	void typeBadDateIs400(String date) {
		ExcelResult res = handler.handleRequest(request("d", "Type", "Sheet1", List.of("A5"), inputs("A5", date)),
				null);
		assertEquals("400", res.getError().getCode());
		assertEquals("d", res.getUuid());
	}

	// ---- KYC: same model as KYCAPI but addressed only by cell references

	@ParameterizedTest(name = "KYC by ref PEP={0} COUNTRY={1} AMOUNT={2} -> {3}")
	@CsvSource({ "true,CY,1000000,125", "true,FR,0,50", "false,KP,100000000,200", "false,JP,100000,25" })
	void kycByReferenceMatchesKycApiByName(String pep, String country, String amount, double expected) {
		double kyc = (Double) single(run("KYC", "ComputeKYC", List.of("C6"),
				inputs("B2", pep, "B3", country, "B4", amount)));
		double api = (Double) single(run("KYCAPI", "ComputeKYC", List.of("SCORE"),
				inputs("PEP", pep, "COUNTRY", country, "AMOUNT", amount)));
		assertEquals(expected, kyc);
		assertEquals(kyc, api);
	}

	@Test
	void kycLabelsAreNotApiNames() {
		// KYC carries bare "@input" comments on the A labels: "PEP" is not resolvable as a name.
		ExcelResult res = handler.handleRequest(
				request("k", "KYC", "ComputeKYC", List.of("C6"), inputs("PEP", "true")), null);
		assertEquals("400", res.getError().getCode());
	}

	@Test
	void sameSheetNameInTwoWorkbooksStaysIndependent() {
		ExcelResult kyc = run("KYC", "ComputeKYC", List.of("C6"), inputs("B3", "KP"));
		ExcelResult api = run("KYCAPI", "ComputeKYC", List.of("C6"), Map.of());
		assertEquals(100.0, single(kyc));
		assertEquals(0.0, single(api));
	}
}
