package net.a.g.excel.engine.cdi;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import net.a.g.excel.exception.ExcelCalculationException;
import net.a.g.excel.exception.ExcelInvalidInputException;
import net.a.g.excel.exception.ExcelResourceNotFoundException;
import net.a.g.excel.model.ExcelCell;

@DisplayName("cellCalculation with cell references")
public class CellCalculationTest extends CdiTestSupport {

	private static final String C6 = "ComputeKYC!C6";

	@BeforeEach
	void loadWorkbooks() {
		load("KYC", "KYCAPI.xlsx");
		load("T1", "Test1.xlsx");
	}

	private Object kyc(Map<String, String> inputs) {
		return engine.cellCalculation("KYC", List.of(C6), inputs).get(0).getValue();
	}

	@ParameterizedTest(name = "Sheet1!A1={0} -> Sheet2!A1={1}, Sheet3!A1={2}")
	@CsvSource({ "4,42.0,16.0", "0,2.0,12.0", "2.5,27.0,14.5" })
	void formulasAcrossSheetsFollowInputs(String a1, double sheet2, double sheet3) {
		List<ExcelCell> cells = engine.cellCalculation("T1", List.of("Sheet2!A1", "Sheet3!A1"), Map.of("Sheet1!A1", a1));
		assertThat(cells.stream().map(ExcelCell::getValue).collect(toList()), contains(sheet2, sheet3));
	}

	@Test
	void stringFormulaFollowsStringInput() {
		List<ExcelCell> cells = engine.cellCalculation("T1", "Sheet2", List.of("B1"), Map.of("Sheet1!B2", "There"),
				false);
		assertEquals("Hello There", cells.get(0).getValue());
		assertEquals("STRING", cells.get(0).getType());
	}

	@Test
	void outputOrderIsKeptAndMissingCellsAreDropped() {
		List<ExcelCell> cells = engine.cellCalculation("T1", List.of("Sheet3!A1", "Sheet1!A999", "Sheet2!A1"),
				Map.of(), false, true);
		assertThat(cells.stream().map(ExcelCell::getAddress).collect(toList()), contains("Sheet3!A1", "Sheet2!A1"));
	}

	@Test
	void outputOnUnknownSheetIsATypedError() {
		ExcelInvalidInputException ex = assertThrows(ExcelInvalidInputException.class,
				() -> engine.cellCalculation("T1", List.of("Sheet3!A1", "Nope!A1"), Map.of(), false, true));
		assertEquals("Nope!A1", ex.getInputName());
	}

	@Test
	void missingColumnOfExistingRowIsBlank() {
		ExcelCell cell = engine.cellCalculation("KYC", List.of("ComputeKYC!D2")).get(0);
		assertEquals("BLANK", cell.getType());
		assertEquals("", cell.getValue());
	}

	@ParameterizedTest(name = "reference ''{0}''")
	@ValueSource(strings = { "c6", "AAAA1", "Compute KYC!C6" })
	void invalidReferencesAreTypedErrors(String ref) {
		assertThrows(ExcelInvalidInputException.class, () -> engine.cellCalculation("KYC", List.of(ref)));
		assertThrows(ExcelInvalidInputException.class, () -> engine.cellCalculation("KYC", List.of(C6), Map.of(ref, "1")));
	}

	@Test
	void nullOrEmptyOutputsAreRejected() {
		assertThrows(IllegalArgumentException.class, () -> engine.cellCalculation("KYC", List.of()));
		assertThrows(IllegalArgumentException.class,
				() -> engine.cellCalculation("KYC", "ComputeKYC", null, Map.of(), false));
	}

	@Test
	void unknownResourceIsNotFound() {
		assertThrows(ExcelResourceNotFoundException.class, () -> engine.cellCalculation("Missing", List.of(C6)));
	}

	@Test
	void inputOnFormulaCellIsIgnored() {
		assertEquals(50.0, kyc(Map.of("ComputeKYC!C2", "999", "ComputeKYC!B2", "true")));
	}

	@Test
	void unknownLookupKeyYieldsAnErrorCell() {
		ExcelCell cell = engine.cellCalculation("KYC", List.of(C6), Map.of("ComputeKYC!B3", "XX")).get(0);
		assertEquals("ERROR", cell.getType());
		assertEquals("#ERROR", cell.getValue());
	}

	@ParameterizedTest(name = "PEP ''{0}'' -> {1}")
	@CsvSource({ "True,50.0", "FALSE,0.0" })
	void booleanInputIsCaseInsensitive(String pep, double expected) {
		assertEquals(expected, kyc(Map.of("ComputeKYC!B2", pep)));
	}

	@Test
	@Disabled("BUG: updateCell uses Boolean.parseBoolean for BOOLEAN cells, so 'yes', '1' or 'tru' are silently "
			+ "computed as FALSE instead of raising ExcelInvalidInputException like numeric/date cells do "
			+ "(InputValidator.validateBooleanValue exists but is never called)")
	void invalidBooleanInputIsATypedError() {
		assertThrows(ExcelInvalidInputException.class, () -> kyc(Map.of("ComputeKYC!B2", "yes")));
	}

	@Test
	void invalidNumericInputIsATypedError() {
		ExcelInvalidInputException ex = assertThrows(ExcelInvalidInputException.class,
				() -> kyc(Map.of("ComputeKYC!B4", "1,000,000")));
		assertEquals("B4", ex.getInputName());
		assertEquals("1,000,000", ex.getInputValue());
	}

	@Test
	void inputOnMissingRowReportsTheReference() {
		ExcelInvalidInputException ex = assertThrows(ExcelInvalidInputException.class,
				() -> kyc(Map.of("ComputeKYC!B100", "1")));
		assertEquals("ComputeKYC!B100", ex.getInputName());
	}

	@Test
	void tooLongValueIsRejected() {
		assertThrows(ExcelInvalidInputException.class, () -> kyc(Map.of("ComputeKYC!B3", "X".repeat(32768))));
	}

	@Test
	void nullInputsAreAcceptedByTheAbsoluteOverload() {
		assertEquals(0.0, engine.cellCalculation("KYC", List.of(C6), null, false, true).get(0).getValue());
	}

	@Test
	void nullInputsAreAcceptedByTheSheetOverload() {
		assertEquals(0.0, engine.cellCalculation("KYC", "ComputeKYC", List.of("C6"), null, false, true).get(0).getValue());
	}

	@Test
	void mapOfCellCalculatedUsesFirstValueOfEachInput() {
		Map<String, ExcelCell> res = engine.mapOfCellCalculated("KYC", "ComputeKYC", new String[] { "C6", "C3" },
				Map.of("B3", List.of("KP", "FR")), false);
		assertEquals(100.0, res.get(C6).getValue());
		assertEquals(100.0, res.get("ComputeKYC!C3").getValue());
	}

	@Test
	void supplierOverloadComputesLazily() {
		List<ExcelCell> cells = engine.cellCalculation(() -> "KYC", () -> List.of(C6),
				() -> Map.of("ComputeKYC!B3", "RU"), false);
		assertEquals(50.0, cells.get(0).getValue());
	}

	@Test
	void calculationsNeverMutateTheStoredTemplate() {
		assertEquals(125.0, kyc(Map.of("ComputeKYC!B2", "true", "ComputeKYC!B3", "CY", "ComputeKYC!B4", "1000000")));
		assertEquals("SUM(C2:C4)", engine.cellCalculation("KYC", List.of(C6)).get(0).getValue());
		assertEquals("IF(B2,50,0)", engine.mapOfFormularCell("KYC", "ComputeKYC").get("C2").getValue());
		assertEquals(0.0, engine.cellCalculation("KYC", List.of(C6), Map.of(), false, true).get(0).getValue());
	}
}
