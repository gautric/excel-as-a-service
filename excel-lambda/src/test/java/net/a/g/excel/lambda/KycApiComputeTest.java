package net.a.g.excel.lambda;

import static net.a.g.excel.lambda.LambdaFixture.cells;
import static net.a.g.excel.lambda.LambdaFixture.inputs;
import static net.a.g.excel.lambda.LambdaFixture.request;
import static net.a.g.excel.lambda.LambdaFixture.single;
import static net.a.g.excel.lambda.LambdaFixture.value;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import net.a.g.excel.model.ExcelCell;
import net.a.g.excel.model.ExcelResult;

/**
 * KYCAPI!ComputeKYC: SCORE (C6) = IF(PEP,50,0) + VLOOKUP(COUNTRY exact) + VLOOKUP(AMOUNT approximate).
 * Defaults in the sheet: PEP=false, COUNTRY=FR, AMOUNT=0.
 */
class KycApiComputeTest {

	private static ExcelLambdaHandler handler;

	@BeforeAll
	static void setup() {
		handler = LambdaFixture.handler("KYCAPI");
	}

	private static ExcelResult run(List<String> outputs, Map<String, String> in) {
		return handler.handleRequest(request("u", "KYCAPI", "ComputeKYC", outputs, in), null);
	}

	private static double score(Map<String, String> in) {
		ExcelResult res = run(List.of("SCORE"), in);
		assertNull(res.getError(), () -> res.getError().getError());
		return (Double) single(res);
	}

	// ---- names -------------------------------------------------------------

	@Test
	void namedInputsAndNamedOutput() {
		assertEquals(125.0, score(inputs("PEP", "true", "COUNTRY", "CY", "AMOUNT", "1000000")));
	}

	@Test
	void onlyPepGivesFifty() {
		assertEquals(50.0, score(inputs("PEP", "true")));
	}

	@Test
	void noInputsUsesSheetDefaults() {
		assertEquals(0.0, score(Map.of()));
	}

	@Test
	void namedOutputCarriesAddressTypeAndMetadata() {
		ExcelResult res = run(List.of("SCORE"), inputs("PEP", "true"));
		ExcelCell c = cells(res).get(0);
		assertEquals("ComputeKYC!C6", c.getAddress());
		assertEquals("NUMERIC", c.getType());
		assertEquals("@output(SCORE)", c.getMetadata());
		assertEquals(1, res.getCount());
	}

	// ---- cell references ---------------------------------------------------

	@Test
	void relativeCellReferencesResolveAgainstSheet() {
		ExcelResult res = run(List.of("C6"), inputs("B2", "true", "B3", "CY", "B4", "1000000"));
		assertEquals(125.0, value(res, "ComputeKYC!C6"));
	}

	@Test
	void absoluteCellReferencesWorkWithoutSheet() {
		ExcelResult res = handler.handleRequest(request("u", "KYCAPI", null, List.of("ComputeKYC!C6"),
				inputs("ComputeKYC!B2", "true", "ComputeKYC!B3", "RU", "ComputeKYC!B4", "100000")), null);
		assertNull(res.getError());
		assertEquals(125.0, single(res));
	}

	@Test
	void namesAndReferencesCanBeMixed() {
		ExcelResult res = run(List.of("SCORE", "C2", "ComputeKYC!C3", "C4"),
				inputs("PEP", "true", "ComputeKYC!B3", "KP", "B4", "10000000"));
		assertNull(res.getError());
		assertEquals(4, res.getCount());
		assertEquals(List.of("ComputeKYC!C6", "ComputeKYC!C2", "ComputeKYC!C3", "ComputeKYC!C4"),
				cells(res).stream().map(ExcelCell::getAddress).toList());
		assertEquals(225.0, value(res, "ComputeKYC!C6"));
		assertEquals(50.0, value(res, "ComputeKYC!C2"));
		assertEquals(100.0, value(res, "ComputeKYC!C3"));
		assertEquals(75.0, value(res, "ComputeKYC!C4"));
	}

	@Test
	void inputCellCanBeReadBackAsOutput() {
		ExcelResult res = run(List.of("B3", "B4", "B2"), inputs("COUNTRY", "JP", "AMOUNT", "42", "PEP", "true"));
		assertEquals("JP", value(res, "ComputeKYC!B3"));
		assertEquals(42.0, value(res, "ComputeKYC!B4"));
		assertEquals("true", value(res, "ComputeKYC!B2"));
	}

	@Test
	void inputOnAnotherSheetChangesLookupTable() {
		// COUNTRY!B3 is the CY weight (25 by default).
		assertEquals(199.0, score(inputs("PEP", "true", "COUNTRY", "CY", "AMOUNT", "1000000", "COUNTRY!B3", "99")));
	}

	@Test
	void inputOnFormulaCellIsIgnoredAndRecomputed() {
		assertEquals(50.0, score(inputs("PEP", "true", "C6", "999")));
	}

	@Test
	void duplicateOutputIsReturnedTwice() {
		ExcelResult res = run(List.of("SCORE", "C6"), inputs("PEP", "true"));
		assertEquals(2, res.getCount());
		assertEquals(List.of(50.0, 50.0), cells(res).stream().map(ExcelCell::getValue).toList());
	}

	@Test
	void outputBeyondUsedRangeIsSilentlyDropped() {
		ExcelResult res = run(List.of("SCORE", "Z99"), Map.of());
		assertNull(res.getError());
		assertEquals(1, res.getCount());
	}

	// ---- default outputs ---------------------------------------------------

	@Test
	void emptyOutputsDefaultToDeclaredOutputs() {
		ExcelResult res = run(List.of(), inputs("PEP", "true", "COUNTRY", "CY", "AMOUNT", "1000000"));
		assertNull(res.getError());
		assertEquals(1, res.getCount());
		assertEquals("ComputeKYC!C6", cells(res).get(0).getAddress());
		assertEquals(125.0, single(res));
	}

	@Test
	void nullOutputsDefaultToDeclaredOutputs() {
		ExcelResult res = run(null, inputs("COUNTRY", "KP"));
		assertNull(res.getError());
		assertEquals(100.0, single(res));
	}

	// ---- isolation between requests ----------------------------------------

	@Test
	void inputsDoNotLeakIntoNextRequest() {
		assertEquals(250.0, score(inputs("PEP", "true", "COUNTRY", "KP", "AMOUNT", "1000000000")));
		assertEquals(0.0, score(Map.of()));
		ExcelResult res = run(List.of("B2", "B3", "B4", "SCORE"), Map.of());
		assertEquals("false", value(res, "ComputeKYC!B2"));
		assertEquals("FR", value(res, "ComputeKYC!B3"));
		assertEquals(0.0, value(res, "ComputeKYC!B4"));
	}

	// ---- parameterized input combinations ----------------------------------

	@ParameterizedTest(name = "COUNTRY={0} -> {1}")
	@CsvSource({ "FR,0", "JP,0", "CY,25", "RU,50", "KP,100" })
	void countryWeight(String country, double expected) {
		assertEquals(expected, score(inputs("COUNTRY", country)));
	}

	@ParameterizedTest(name = "AMOUNT={0} -> {1}")
	@CsvSource({ "0,0", "99999,0", "100000,25", "100000.5,25", "999999,25", "1000000,50", "9999999.99,50",
			"10000000,75", "99999999,75", "100000000,100", "5E9,100" })
	void amountBands(String amount, double expected) {
		assertEquals(expected, score(inputs("AMOUNT", amount)));
	}

	@ParameterizedTest(name = "PEP={0} -> {1}")
	@CsvSource({ "true,50", "TRUE,50", "True,50", "false,0", "yes,0", "1,0", "'',0" })
	void pepParsing(String pep, double expected) {
		// Boolean.parseBoolean semantics: only a case-insensitive "true" is true.
		assertEquals(expected, score(inputs("PEP", pep)));
	}

	static Stream<Arguments> combinations() {
		return Stream.of(
				Arguments.of("false", "FR", "0", 0.0),
				Arguments.of("true", "FR", "0", 50.0),
				Arguments.of("false", "CY", "100000", 50.0),
				Arguments.of("true", "CY", "1000000", 125.0),
				Arguments.of("true", "RU", "10000000", 175.0),
				Arguments.of("false", "KP", "100000000", 200.0),
				Arguments.of("true", "KP", "100000000", 250.0),
				Arguments.of("true", "JP", "500000", 75.0),
				Arguments.of("false", "RU", "99999", 50.0));
	}

	@ParameterizedTest(name = "by name PEP={0} COUNTRY={1} AMOUNT={2} -> {3}")
	@MethodSource("combinations")
	void combinationsByName(String pep, String country, String amount, double expected) {
		assertEquals(expected, score(inputs("PEP", pep, "COUNTRY", country, "AMOUNT", amount)));
	}

	@ParameterizedTest(name = "by ref PEP={0} COUNTRY={1} AMOUNT={2} -> {3}")
	@MethodSource("combinations")
	void combinationsByReferenceAgreeWithNames(String pep, String country, String amount, double expected) {
		ExcelResult res = run(List.of("C6"), inputs("B2", pep, "ComputeKYC!B3", country, "B4", amount));
		assertNull(res.getError());
		assertEquals(expected, single(res));
	}

	@ParameterizedTest(name = "unknown country {0} -> #ERROR")
	@CsvSource({ "US", "XX", "' FR'" })
	void unknownCountryYieldsFormulaErrorNotFailure(String country) {
		ExcelResult res = run(List.of("C3", "SCORE"), inputs("COUNTRY", country));
		assertNull(res.getError());
		assertNotNull(res.getCells());
		assertEquals("#ERROR", value(res, "ComputeKYC!C6"));
		assertEquals("ERROR", cells(res).get(1).getType());
	}
}
