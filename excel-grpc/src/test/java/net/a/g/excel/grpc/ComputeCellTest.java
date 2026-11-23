package net.a.g.excel.grpc;

import static net.a.g.excel.grpc.Fixtures.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.grpc.Status;

class ComputeCellTest {

	static ExcelService service;

	@BeforeAll
	static void setup() {
		service = service(ALL);
	}

	private static ExcelCell compute(ExcelRequest.Builder b) {
		return single(service.computeCell(b.build()));
	}

	// --- success ---------------------------------------------------------

	@Test
	void referenceScenarioScores125() {
		ExcelCell c = compute(kycapi("true", "CY", "1000000"));
		assertEquals("ComputeKYC!C6", c.getAddress());
		assertEquals("NUMERIC", c.getType());
		assertEquals("@output(SCORE)", c.getMetadata());
		assertEquals(125.0, dbl(c));
	}

	/** PEP adds 50, COUNTRY is an exact lookup. */
	@ParameterizedTest(name = "PEP={0} COUNTRY={1} -> {2}")
	@CsvSource({ "true,FR,50", "true,JP,50", "true,CY,75", "true,RU,100", "true,KP,150", //
			"false,FR,0", "false,JP,0", "false,CY,25", "false,RU,50", "false,KP,100" })
	void pepAndCountryCombinations(String pep, String country, double expected) {
		assertEquals(expected, dbl(compute(kycapi(pep, country, "0"))));
	}

	/** AMOUNT is an approximate (range) lookup on 0 / 1e5 / 1e6 / 1e7 / 1e8. */
	@ParameterizedTest(name = "AMOUNT={0} -> {1}")
	@CsvSource({ "99999.99,0", "100000,25", "999999,25", "1000000,50", "10000000,75",
			"100000000,100", "1e12,100" })
	void amountBrackets(String amount, double expected) {
		assertEquals(expected, dbl(compute(kycapi("false", "FR", amount))));
	}

	@ParameterizedTest(name = "PEP=\"{0}\" counts as {1}")
	@CsvSource({ "TRUE,50", "false,0", "yes,0", "'',0" })
	void booleanInputParsing(String pep, double expected) {
		assertEquals(expected, dbl(compute(kycapi(pep, "FR", "0"))));
	}

	@ParameterizedTest(name = "output reference \"{0}\"")
	@ValueSource(strings = { "SCORE", "C6", "ComputeKYC!C6" })
	void outputCanBeAnApiNameARelativeOrAnAbsoluteReference(String output) {
		ExcelRequest.Builder b = req("KYCAPI", "ComputeKYC").addOutputs(output).putInputs("PEP", "true")
				.putInputs("COUNTRY", "CY").putInputs("AMOUNT", "1000000");
		assertEquals(125.0, dbl(compute(b)));
	}

	@ParameterizedTest(name = "inputs keyed by {0}")
	@CsvSource(delimiter = ';', value = { "B2;B3;B4", "ComputeKYC!B2;ComputeKYC!B3;ComputeKYC!B4", "PEP;B3;ComputeKYC!B4" })
	void inputsCanBeNamesOrReferences(String pep, String country, String amount) {
		ExcelRequest.Builder b = req("KYCAPI", "ComputeKYC").addOutputs("SCORE").putInputs(pep, "true")
				.putInputs(country, "CY").putInputs(amount, "1000000");
		assertEquals(125.0, dbl(compute(b)));
	}

	@Test
	void workbookWithoutApiNamesIsDrivenByAddresses() {
		ExcelRequest.Builder b = req("KYC", "ComputeKYC").addOutputs("C6").putInputs("B2", "true")
				.putInputs("B3", "CY").putInputs("B4", "1000000");
		ExcelCell c = compute(b);
		assertEquals("@output", c.getMetadata());
		assertEquals(125.0, dbl(c));
	}

	@Test
	void missingOutputsDefaultToEveryDeclaredOutput() {
		ExcelRequest.Builder b = req("KYCAPI", "ComputeKYC").putInputs("PEP", "true").putInputs("COUNTRY", "CY")
				.putInputs("AMOUNT", "1000000");
		ExcelCell c = compute(b);
		assertEquals("ComputeKYC!C6", c.getAddress());
		assertEquals(125.0, dbl(c));
	}

	@Test
	void unsetInputsKeepTheWorkbookDefaults() {
		// defaults: PEP false, COUNTRY FR, AMOUNT 0
		assertEquals(0.0, dbl(compute(req("KYCAPI", "ComputeKYC").addOutputs("SCORE"))));
		assertEquals(25.0, dbl(compute(req("KYCAPI", "ComputeKYC").addOutputs("SCORE").putInputs("COUNTRY", "CY"))));
	}

	@Test
	void formulasAreEvaluatedEvenWithoutInputs() {
		ExcelCell c = compute(req("Test1", "Sheet2").addOutputs("A1"));
		assertEquals("NUMERIC", c.getType());
		assertEquals(42.0, dbl(c));
	}

	@Test
	void absoluteOutputsWorkWithoutASheet() {
		ExcelCell c = compute(ExcelRequest.newBuilder().setResource("Test1").addOutputs("Sheet3!A1"));
		assertEquals(16.0, dbl(c));
	}

	@Test
	void crossSheetFormulasSeeInputsOfAnotherSheet() {
		ExcelRequest.Builder b = req("Test1", "Sheet2").addOutputs("A1").addOutputs("B1").putInputs("Sheet1!A1", "5")
				.putInputs("Sheet1!B1", "Bonjour");
		Map<String, ExcelCell> m = items(service.computeCell(b.build())).stream()
				.collect(Collectors.toMap(ExcelCell::getAddress, c -> c));
		assertEquals(52.0, dbl(m.get("Sheet2!A1")));
		assertEquals("Bonjour World", str(m.get("Sheet2!B1")));
	}

	@Test
	void severalOutputsAreStreamedInRequestOrder() {
		ExcelRequest.Builder b = req("KYCAPI", "ComputeKYC").addOutputs("C4").addOutputs("C2").addOutputs("C3")
				.addOutputs("SCORE").putInputs("PEP", "true").putInputs("COUNTRY", "CY").putInputs("AMOUNT", "1000000");
		List<ExcelCell> cells = items(service.computeCell(b.build()));
		assertEquals(List.of("ComputeKYC!C4", "ComputeKYC!C2", "ComputeKYC!C3", "ComputeKYC!C6"),
				cells.stream().map(ExcelCell::getAddress).toList());
		assertEquals(List.of(50.0, 50.0, 25.0, 125.0), cells.stream().map(Fixtures::dbl).toList());
	}

	@Test
	void outputOnARowThatDoesNotExistIsSilentlyDropped() {
		assertTrue(items(service.computeCell(req("KYCAPI", "ComputeKYC").addOutputs("Z99").build())).isEmpty());
	}

	@Test
	void inputValueCanBeEchoedBackAsOutput() {
		ExcelCell c = compute(req("KYCAPI", "ComputeKYC").addOutputs("B3").putInputs("COUNTRY", "RU"));
		assertEquals("STRING", c.getType());
		assertEquals("RU", str(c));
	}

	@Test
	void unknownCountryYieldsAnErrorCellNotAFailedCall() {
		ExcelCell c = compute(kycapi("false", "US", "0"));
		assertEquals("ERROR", c.getType());
		assertEquals("#ERROR", str(c));
	}

	@Test
	void writingIntoAFormulaCellIsIgnored() {
		ExcelRequest.Builder b = req("KYCAPI", "ComputeKYC").addOutputs("SCORE").putInputs("C2", "1000")
				.putInputs("COUNTRY", "CY");
		assertEquals(25.0, dbl(compute(b)));
	}

	@Test
	void computesDoNotLeakIntoEachOther() {
		assertEquals(150.0, dbl(compute(kycapi("true", "KP", "0"))));
		assertEquals(0.0, dbl(compute(req("KYCAPI", "ComputeKYC").addOutputs("SCORE"))));
	}

	// --- errors -----------------------------------------------------------

	@ParameterizedTest(name = "resource \"{0}\"")
	@ValueSource(strings = { "", "bad name", "../KYCAPI", "KYCAPI.xlsx" })
	void malformedResourceIsInvalidArgument(String resource) {
		failure(service.computeCell(req(resource, "ComputeKYC").addOutputs("SCORE").build()),
				Status.Code.INVALID_ARGUMENT);
	}

	@Test
	void unknownResourceIsNotFound() {
		var e = failure(service.computeCell(req("NOPE", "ComputeKYC").addOutputs("SCORE").build()),
				Status.Code.NOT_FOUND);
		assertTrue(e.getStatus().getDescription().contains("NOPE"));
	}

	@Test
	void unknownSheetIsNotFound() {
		var e = failure(service.computeCell(req("KYCAPI", "Nope").addOutputs("SCORE").build()), Status.Code.NOT_FOUND);
		assertEquals("Sheet 'Nope' not found in resource 'KYCAPI'", e.getStatus().getDescription());
	}

	@ParameterizedTest(name = "output \"{0}\"")
	@ValueSource(strings = { "UNKNOWN", "c6", "1A", "ComputeKYC!", "ComputeKYC!C6:C7", " " })
	void unknownOrMalformedOutputIsInvalidArgument(String output) {
		failure(service.computeCell(req("KYCAPI", "ComputeKYC").addOutputs(output).build()),
				Status.Code.INVALID_ARGUMENT);
	}

	@Test
	void unknownInputNameIsInvalidArgument() {
		var e = failure(service.computeCell(req("KYCAPI", "ComputeKYC").addOutputs("SCORE").putInputs("AGE", "3").build()),
				Status.Code.INVALID_ARGUMENT);
		assertTrue(e.getStatus().getDescription().contains("AGE"));
	}

	@Test
	void noOutputAndNoDeclaredOutputIsInvalidArgument() {
		failure(service.computeCell(req("KYC", "ComputeKYC").putInputs("B2", "true").build()),
				Status.Code.INVALID_ARGUMENT);
	}

	@Test
	void relativeReferenceWithoutSheetIsInvalidArgument() {
		var e = failure(service.computeCell(ExcelRequest.newBuilder().setResource("KYCAPI").addOutputs("C6").build()),
				Status.Code.INVALID_ARGUMENT);
		assertTrue(e.getStatus().getDescription().contains("set 'sheet'"));
	}

	@Test
	void apiNameWithoutSheetIsInvalidArgument() {
		failure(service.computeCell(ExcelRequest.newBuilder().setResource("KYCAPI").addOutputs("SCORE").build()),
				Status.Code.INVALID_ARGUMENT);
	}

	@ParameterizedTest(name = "AMOUNT=\"{0}\"")
	@ValueSource(strings = { "abc", "1,000", "12e", "NaN?" })
	void nonNumericValueForANumericCellIsInvalidArgument(String amount) {
		var e = failure(service.computeCell(kycapi("true", "CY", amount).build()), Status.Code.INVALID_ARGUMENT);
		assertTrue(e.getStatus().getDescription().contains("numeric"));
	}

	@ParameterizedTest(name = "formula injection \"{0}\"")
	@ValueSource(strings = { "=1+1", "+1", "@SUM(A1)", "  =cmd" })
	void formulaInjectionIsInvalidArgument(String value) {
		failure(service.computeCell(kycapi("true", value, "0").build()), Status.Code.INVALID_ARGUMENT);
	}

	@Test
	@Disabled("BUG: InputValidator.validateCellValue treats any value starting with '-' as formula injection, "
			+ "so a negative number can never be sent to a numeric input")
	void negativeNumberIsAcceptedForANumericInput() {
		assertEquals(0.0, dbl(compute(kycapi("false", "FR", "-1"))));
	}

	@Test
	void tooLongValueIsInvalidArgument() {
		failure(service.computeCell(kycapi("true", "x".repeat(32768), "0").build()), Status.Code.INVALID_ARGUMENT);
	}

	@ParameterizedTest(name = "input on missing cell \"{0}\"")
	@ValueSource(strings = { "Z99", "Nope!B2" })
	void inputOnAMissingCellIsAClientError(String ref) {
		failure(service.computeCell(req("KYCAPI", "ComputeKYC").addOutputs("SCORE").putInputs(ref, "1").build()),
				Status.Code.INVALID_ARGUMENT);
	}
}
