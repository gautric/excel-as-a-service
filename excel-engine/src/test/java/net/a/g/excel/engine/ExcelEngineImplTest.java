package net.a.g.excel.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.poi.ss.usermodel.CellType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import net.a.g.excel.exception.ExcelCalculationException;
import net.a.g.excel.exception.ExcelInvalidInputException;
import net.a.g.excel.exception.ExcelResourceNotFoundException;
import net.a.g.excel.model.ExcelCell;
import net.a.g.excel.model.ExcelRequest;
import net.a.g.excel.model.ExcelResult;
import net.a.g.excel.model.ExcelSheet;
import net.a.g.excel.param.ExcelParameterImpl;

/** Behaviour of every public method of ExcelEngineImpl against the sample workbooks. */
public class ExcelEngineImplTest extends ExcelUnitTest {

	private static final String CK = "ComputeKYC";

	@BeforeEach
	@Override
	public void setup() throws IOException {
		super.setup();
		for (String name : List.of("KYC", "KYCAPI", "Test1", "Type", "Primary", "Secondary")) {
			assertTrue(loader.injectResource(name, name + ".xlsx",
					FileUtils.openInputStream(new File("../sample/" + name + ".xlsx"))));
		}
	}

	private ExcelEngineImpl impl() {
		return (ExcelEngineImpl) engine;
	}

	private static ExcelRequest request(String resource, String sheet, List<String> outputs, Map<String, String> inputs) {
		ExcelRequest r = new ExcelRequest("uuid-test");
		r.setResource(resource);
		r.setSheet(sheet);
		r.setOutputs(outputs);
		r.setInputs(inputs);
		return r;
	}

	private ExcelCell one(String resource, String output, Map<String, String> inputs) {
		List<ExcelCell> cells = engine.cellCalculation(resource, List.of(output), inputs);
		assertEquals(1, cells.size());
		return cells.get(0);
	}

	private static Set<String> addresses(List<ExcelCell> cells) {
		return cells.stream().map(ExcelCell::getAddress).collect(Collectors.toSet());
	}

	// ------------------------------------------------------------------ compute()

	@ParameterizedTest(name = "PEP={0} COUNTRY={1} AMOUNT={2} -> {3}")
	@CsvSource({
			"true,FR,0,50.0",
			"false,CY,0,25.0",
			"false,KP,0,100.0",
			"false,FR,1000000,50.0",
			"true,CY,1000000,125.0",
			"TRUE,RU,50000,100.0" })

	void computeScoreTable(String pep, String country, String amount, double expected) {
		ExcelResult res = engine.compute(request("KYCAPI", CK, List.of("SCORE"),
				Map.of("PEP", pep, "COUNTRY", country, "AMOUNT", amount)));
		assertEquals(1, res.getCount());
		assertEquals(expected, res.getCells().iterator().next().getValue());
	}

	@Test
	void computeResultCarriesAddressTypeAndMetadata() {
		ExcelCell c = engine.compute(request("KYCAPI", CK, List.of("SCORE"), Map.of("PEP", "true")))
				.getCells().iterator().next();
		assertEquals("ComputeKYC!C6", c.getAddress());
		assertEquals("NUMERIC", c.getType());
		assertEquals("@output(SCORE)", c.getMetadata());
	}

	@Test
	void computeCopiesUuid() {
		ExcelRequest r = request("KYCAPI", CK, List.of("SCORE"), Map.of());
		r.setUuid("my-uuid");
		assertEquals("my-uuid", engine.compute(r).getUuid());
	}

	@Test
	void computeWithoutInputsStillEvaluates() {
		ExcelCell c = engine.compute(request("KYCAPI", CK, List.of("SCORE"), null)).getCells().iterator().next();
		assertEquals(0.0, c.getValue());
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "  " })
	void computeWithoutSheetNeedsAbsoluteReferences(String sheet) {
		ExcelResult res = engine.compute(request("KYCAPI", sheet, List.of("ComputeKYC!C6"),
				Map.of("ComputeKYC!B2", "true")));
		assertEquals(50.0, res.getCells().iterator().next().getValue());
	}

	@Test
	void computeWithoutSheetRejectsRelativeOutput() {
		ExcelInvalidInputException e = assertThrows(ExcelInvalidInputException.class,
				() -> engine.compute(request("KYCAPI", null, List.of("C6"), Map.of())));
		assertTrue(e.getMessage().contains("Unknown output 'C6'"));
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "" })
	void computeDefaultsToAllOutputsWhenNoneRequested(String marker) {
		List<String> outputs = marker == null ? null : List.of();
		ExcelResult res = engine.compute(request("KYCAPI", CK, outputs, Map.of("COUNTRY", "KP")));
		assertEquals(1, res.getCount());
		assertEquals(100.0, res.getCells().iterator().next().getValue());
	}

	@Test
	void computeNoOutputsAndNoApiOutputsFails() {
		ExcelInvalidInputException e = assertThrows(ExcelInvalidInputException.class,
				() -> engine.compute(request("KYCAPI", "COUNTRY", List.of(), Map.of())));
		assertEquals("At least one output is required", e.getMessage());
	}

	@Test
	void computeUnnamedOutputCommentIsNotAnApiOutput() {
		// KYC.xlsx carries a bare "@output" comment with no name.
		assertThrows(ExcelInvalidInputException.class,
				() -> engine.compute(request("KYC", CK, List.of(), Map.of())));
	}

	@Test
	void computeMixesApiNamesAndCellReferences() {
		List<ExcelCell> cells = new ArrayList<>(engine.compute(request("KYCAPI", CK,
				List.of("SCORE", "C2", "COUNTRY!B5"), Map.of("PEP", "true", "B3", "RU"))).getCells());
		assertEquals(3, cells.size());
		assertEquals(100.0, cells.get(0).getValue());
		assertEquals(50.0, cells.get(1).getValue());
		assertEquals(100.0, cells.get(2).getValue());
		assertEquals("COUNTRY!B5", cells.get(2).getAddress());
	}

	@Test
	void computeOutputOnMissingRowIsDropped() {
		ExcelResult res = engine.compute(request("KYCAPI", CK, List.of("Z100"), Map.of()));
		assertEquals(0, res.getCount());
		assertTrue(res.getCells().isEmpty());
	}

	@Test
	void computeOutputOnMissingColumnOfExistingRowIsBlank() {
		ExcelCell c = engine.compute(request("KYCAPI", CK, List.of("Z1"), Map.of())).getCells().iterator().next();
		assertEquals("ComputeKYC!Z1", c.getAddress());
		assertEquals("", c.getValue());
		assertEquals("BLANK", c.getType());
	}

	@Test
	void computeUnknownCountryYieldsError() {
		ExcelCell c = engine.compute(request("KYCAPI", CK, List.of("SCORE"), Map.of("COUNTRY", "XX")))
				.getCells().iterator().next();
		assertEquals("#ERROR", c.getValue());
	}

	@Test
	void computeBooleanInputIsLenient() {
		// Boolean.parseBoolean: anything but "true" (any case) is false.
		ExcelCell c = engine.compute(request("KYCAPI", CK, List.of("SCORE"), Map.of("PEP", "yes")))
				.getCells().iterator().next();
		assertEquals(0.0, c.getValue());
	}

	@Test
	void computeNullRequestThrows() {
		ExcelInvalidInputException e = assertThrows(ExcelInvalidInputException.class, () -> engine.compute(null));
		assertEquals("Request cannot be null", e.getMessage());
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "KYC.xlsx" })
	void computeInvalidResourceName(String resource) {
		assertThrows(ExcelInvalidInputException.class,
				() -> engine.compute(request(resource, CK, List.of("SCORE"), Map.of())));
	}

	@Test
	void computeUnknownResource() {
		ExcelResourceNotFoundException e = assertThrows(ExcelResourceNotFoundException.class,
				() -> engine.compute(request("NOPE", CK, List.of("SCORE"), Map.of())));
		assertEquals("NOPE", e.getResourceName());
	}

	@Test
	void computeUnknownSheet() {
		ExcelResourceNotFoundException e = assertThrows(ExcelResourceNotFoundException.class,
				() -> engine.compute(request("KYCAPI", "Nope", List.of("SCORE"), Map.of())));
		assertEquals("Sheet 'Nope' not found in resource 'KYCAPI'", e.getMessage());
		assertEquals("KYCAPI", e.getResourceName());
	}

	@ParameterizedTest
	@ValueSource(strings = { "Bad Sheet!A1" })
	void computeUnknownOutputName(String output) {
		ExcelInvalidInputException e = assertThrows(ExcelInvalidInputException.class,
				() -> engine.compute(request("KYCAPI", CK, List.of(output), Map.of())));
		assertEquals(output, e.getInputName());
	}

	@Test
	void computeBlankOutputName() {
		List<String> outs = new ArrayList<>();
		outs.add(" ");
		ExcelInvalidInputException e = assertThrows(ExcelInvalidInputException.class,
				() -> engine.compute(request("KYCAPI", CK, outs, Map.of())));
		assertEquals("Empty output name", e.getMessage());
	}

	@Test
	void computeUnknownInputName() {
		ExcelInvalidInputException e = assertThrows(ExcelInvalidInputException.class,
				() -> engine.compute(request("KYCAPI", CK, List.of("SCORE"), Map.of("WHO", "x"))));
		assertEquals("WHO", e.getInputName());
		assertTrue(e.getMessage().contains("not an @input name nor a cell reference"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "-5" })
	void computeRejectsFormulaInjection(String value) {
		ExcelInvalidInputException e = assertThrows(ExcelInvalidInputException.class,
				() -> engine.compute(request("KYCAPI", CK, List.of("SCORE"), Map.of("AMOUNT", value))));
		assertEquals("ComputeKYC!B4", e.getInputName());
	}

	@ParameterizedTest
	@ValueSource(strings = { "1,000" })
	void computeRejectsNonNumericAmount(String value) {
		ExcelInvalidInputException e = assertThrows(ExcelInvalidInputException.class,
				() -> engine.compute(request("KYCAPI", CK, List.of("SCORE"), Map.of("AMOUNT", value))));
		// updateCell reports the address without the sheet name
		assertEquals("B4", e.getInputName());
		assertEquals(value, e.getInputValue());
	}

	@Test
	void computeInputOnMissingRowIsInvalidInput() {
		assertThrows(ExcelInvalidInputException.class,
				() -> engine.compute(request("KYCAPI", CK, List.of("SCORE"), Map.of("B50", "x"))));
	}

	@Test
	void computeGlobalCrossWorkbookReference() {
		ExcelRequest r = request("Primary", "Feuil1", List.of("A1", "B1"), Map.of("C1", "0"));
		r.setGlobal(true);
		List<ExcelCell> cells = new ArrayList<>(engine.compute(r).getCells());
		assertEquals(42.0, cells.get(0).getValue());
		assertEquals(44.0, cells.get(1).getValue());
	}

	@Test
	void computeSheetNameCaseIsConsistent() {
		ExcelResult res = engine.compute(request("KYCAPI", "computekyc", List.of("SCORE"), Map.of("PEP", "true")));
		assertEquals(50.0, res.getCells().iterator().next().getValue());
	}

	// ----------------------------------------------- compute() on Test1 / Type workbooks

	@ParameterizedTest
	@CsvSource({ "1.5,2,0.5,3.5" })
	void test1ArithmeticFormula(String a1, String a3, String a2, double expected) {
		ExcelResult res = engine.compute(request("Test1", "Sheet1", List.of("Sheet2!A1"),
				Map.of("A1", a1, "A3", a3, "A2", a2)));
		assertEquals(expected, res.getCells().iterator().next().getValue());
	}

	@ParameterizedTest
	@CsvSource({ "Bye,All,Bye All" })
	void test1Concatenate(String b1, String b2, String expected) {
		ExcelCell c = engine.compute(request("Test1", "Sheet1", List.of("Sheet2!B1"), Map.of("B1", b1, "B2", b2)))
				.getCells().iterator().next();
		assertEquals(expected, c.getValue());
		assertEquals("STRING", c.getType());
	}

	@Test
	void typeDateCellFollowsConfiguredFormat() {
		ExcelParameterImpl p = new ExcelParameterImpl();
		p.setFormatDate("dd/MM/yyyy");
		impl().setParameter(p);
		ExcelCell c = engine.compute(request("Type", "Sheet1", List.of("A5"), Map.of())).getCells().iterator().next();
		assertEquals("01/01/2021", c.getValue());
	}

	@ParameterizedTest
	@ValueSource(strings = { "2024-02-29" })
	void typeDateInputRoundTrips(String date) {
		ExcelCell c = engine.compute(request("Type", "Sheet1", List.of("A5"), Map.of("A5", date))).getCells()
				.iterator().next();
		assertEquals(date, c.getValue());
	}

	@ParameterizedTest
	@ValueSource(strings = { "15/06/2022" })
	void typeDateInputInvalid(String date) {
		ExcelInvalidInputException e = assertThrows(ExcelInvalidInputException.class,
				() -> engine.compute(request("Type", "Sheet1", List.of("A5"), Map.of("A5", date))));
		assertEquals("A5", e.getInputName());
		assertTrue(e.getMessage().contains("Expected a date with format yyyy-MM-dd"));
	}

	@ParameterizedTest
	@CsvSource({ "A6,7,7.0,NUMERIC", "C3,filled,filled,STRING" })
	void typeInputOverwritesCell(String cell, String value, String expected, String type) {
		ExcelCell c = engine.compute(request("Type", "Sheet1", List.of(cell), Map.of(cell, value))).getCells()
				.iterator().next();
		assertEquals(expected, String.valueOf(c.getValue()));
		assertEquals(type, c.getType());
	}

	@Test
	void typeInputOnFormulaCellIsIgnored() {
		ExcelCell c = engine.compute(request("Type", "Sheet1", List.of("A9"), Map.of("A9", "1"))).getCells()
				.iterator().next();
		assertEquals(42.0, c.getValue());
	}

	@Test
	void typeNumericInputRejectsText() {
		ExcelInvalidInputException e = assertThrows(ExcelInvalidInputException.class,
				() -> engine.compute(request("Type", "Sheet1", List.of("A9"), Map.of("A1", "ten"))));
		assertEquals("A1", e.getInputName());
		assertTrue(e.getMessage().contains("Expected a numeric value"));
	}

	// ------------------------------------------------------ cellCalculation (raw)

	@ParameterizedTest
	@CsvSource({
			"KYC,ComputeKYC!B2,false,BOOLEAN",
			"KYC,ComputeKYC!B3,FR,STRING",
			"KYC,ComputeKYC!C6,SUM(C2:C4),FORMULA",
			"Type,Sheet1!A5,2021-01-01,DATE",
			"Type,Sheet1!C3,'',BLANK" })

	void rawCellValueAndType(String resource, String ref, String value, String type) {
		ExcelCell c = engine.cellCalculation(resource, List.of(ref)).get(0);
		assertEquals(ref, c.getAddress());
		assertEquals(value, String.valueOf(c.getValue()));
		assertEquals(type, c.getType());
	}

	@Test
	void rawCellCarriesCommentWithoutNewlines() {
		ExcelCell c = engine.cellCalculation("KYCAPI", List.of("ComputeKYC!B2")).get(0);
		assertEquals("@input(PEP)", c.getMetadata());
	}

	@Test
	void forceEvaluatesWithoutInputs() {
		ExcelCell c = engine.cellCalculation("KYC", List.of("ComputeKYC!C6"), Map.of(), false, true).get(0);
		assertEquals(0.0, c.getValue());
		assertEquals("NUMERIC", c.getType());
	}

	@Test
	void globalFlagWithoutInputsDoesNotEvaluate() {
		ExcelCell c = engine.cellCalculation("KYC", List.of("ComputeKYC!C6"), true).get(0);
		assertEquals("FORMULA", c.getType());
	}

	@ParameterizedTest
	@NullAndEmptySource
	void cellCalculationRejectsMissingOutputs(List<String> outputs) {
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
				() -> engine.cellCalculation("KYC", outputs));
		assertEquals("Output list cannot be null or empty", e.getMessage());
	}

	@ParameterizedTest
	@ValueSource(strings = { "Compute KYC!C6" })
	void cellCalculationRejectsInvalidOutputReference(String ref) {
		assertThrows(ExcelInvalidInputException.class, () -> engine.cellCalculation("KYC", List.of(ref)));
	}

	@Test
	void cellCalculationRejectsInjectedInputValue() {
		ExcelInvalidInputException e = assertThrows(ExcelInvalidInputException.class,
				() -> engine.cellCalculation("KYC", List.of("ComputeKYC!C6"), Map.of("ComputeKYC!B3", "=HYPERLINK(1)")));
		assertEquals("ComputeKYC!B3", e.getInputName());
	}

	@Test
	void cellCalculationUnknownResource() {
		assertThrows(ExcelResourceNotFoundException.class, () -> engine.cellCalculation("NOPE", List.of("S!A1")));
	}

	@Test
	void cellCalculationInputOnUnknownSheetFails() {
		ExcelInvalidInputException e = assertThrows(ExcelInvalidInputException.class,
				() -> engine.cellCalculation("KYC", List.of("ComputeKYC!C6"), Map.of("Nope!A1", "1")));
		assertEquals("Nope!A1", e.getInputName());
	}

	@Test
	void cellCalculationSupplierVariant() {
		List<ExcelCell> cells = engine.cellCalculation(() -> "KYC", () -> List.of("ComputeKYC!C6"),
				() -> Map.of("ComputeKYC!B2", "true", "ComputeKYC!B4", "100000"), false);
		assertEquals(75.0, cells.get(0).getValue());
	}

	// ---------------------------------------------- cellCalculation with sheet

	@Test
	void sheetVariantResolvesRelativeReferences() {
		List<ExcelCell> cells = engine.cellCalculation("KYC", CK, List.of("C6", "COUNTRY!B1"),
				Map.of("B2", "true", "B3", "KP"), false);
		assertEquals("ComputeKYC!C6", cells.get(0).getAddress());
		assertEquals(150.0, cells.get(0).getValue());
		assertEquals("COUNTRY!B1", cells.get(1).getAddress());
	}

	@Test
	void sheetVariantForce() {
		ExcelCell c = engine.cellCalculation("KYC", CK, List.of("C6"), Map.of(), false, true).get(0);
		assertEquals(0.0, c.getValue());
	}

	@Test
	void sheetVariantNullOutputRejected() {
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
				() -> engine.cellCalculation("KYC", CK, null, Map.of(), false));
		assertEquals("Output list cannot be null", e.getMessage());
	}

	@ParameterizedTest
	@ValueSource(strings = { "a:b" })
	void sheetVariantInvalidSheetName(String sheet) {
		assertThrows(ExcelInvalidInputException.class,
				() -> engine.cellCalculation("KYC", sheet, List.of("C6"), Map.of(), false));
	}

	@Test
	void sheetVariantNullInputsAccepted() {
		assertEquals(1, engine.cellCalculation("KYC", CK, List.of("C6"), null, false).size());
	}

	// ---------------------------------------------------- mapOfCellCalculated

	@Test
	void mapOfCellCalculatedKeysByAddressAndTakesFirstInputValue() {
		Map<String, ExcelCell> m = engine.mapOfCellCalculated("KYC", CK, new String[] { "C6", "C2" },
				Map.of("B2", List.of("true", "false")), false);
		assertEquals(Set.of("ComputeKYC!C6", "ComputeKYC!C2"), m.keySet());
		assertEquals(50.0, m.get("ComputeKYC!C6").getValue());
		assertEquals(50.0, m.get("ComputeKYC!C2").getValue());
	}

	@Test
	void cellCalculationOldDelegates() {
		List<ExcelCell> cells = impl().cellCalculationOld("KYC", CK, List.of("C6"),
				Map.of("B3", List.of("RU")), false);
		assertEquals(50.0, cells.get(0).getValue());
	}

	// --------------------------------------------------------------- sheets

	@Test
	void listOfSheetInWorkbookOrder() {
		assertEquals(List.of("ComputeKYC", "COUNTRY", "AMOUNT"),
				engine.listOfSheet("KYC").stream().map(ExcelSheet::getName).toList());
	}

	@Test
	void listOfSheetUnknown() {
		assertThrows(ExcelResourceNotFoundException.class, () -> engine.listOfSheet("NOPE"));
	}

	@Test
	void getSheetReturnsNamedSheet() {
		assertEquals("COUNTRY", engine.getSheet("KYC", "COUNTRY").getName());
	}

	@Test
	void getSheetUnknownSheet() {
		ExcelResourceNotFoundException e = assertThrows(ExcelResourceNotFoundException.class,
				() -> engine.getSheet("KYC", "Nope"));
		assertEquals("Sheet not found: Nope", e.getMessage());
	}

	@ParameterizedTest
	@CsvSource(value = {
			"KYC|ComputeKYC|true",
			"Test1|Sheet4|false",
			"bad name|Sheet1|false" }, delimiter = '|')
	void isSheetExists(String resource, String sheet, boolean expected) {
		assertEquals(expected, engine.isSheetExists(resource, sheet));
	}

	@Test
	void isSheetExistsNullArguments() {
		assertFalse(engine.isSheetExists(null, "ComputeKYC"));
		assertFalse(engine.isSheetExists("KYC", null));
	}

	// ------------------------------------------------------------- cell listing

	@Test
	void mapOfFormularCellKeysWithoutSheet() {
		Map<String, ExcelCell> m = engine.mapOfFormularCell("KYC", CK);
		assertEquals(Set.of("C2", "C3", "C4", "C6"), m.keySet());
		assertEquals("ComputeKYC!C6", m.get("C6").getAddress());
		assertEquals("FORMULA", m.get("C6").getType());
	}

	@ParameterizedTest
	@CsvSource({ "KYC,AMOUNT,3" })
	void mapOfFormularCellCount(String resource, String sheet, int count) {
		assertEquals(count, engine.mapOfFormularCell(resource, sheet).size());
	}

	@Test
	void mapOfCellWithPredicate() {
		Map<String, ExcelCell> m = engine.mapOfCell("Test1", "Sheet1", c -> c.getCellType() == CellType.STRING);
		assertEquals(Set.of("B1", "B2"), m.keySet());
		assertEquals("Hello", m.get("B1").getValue());
	}

	@Test
	void mapOfCellNullPredicate() {
		assertThrows(IllegalArgumentException.class, () -> engine.mapOfCell("KYC", CK, null));
	}

	@Test
	void listOfCellNullPredicate() {
		assertThrows(IllegalArgumentException.class, () -> engine.listOfCell("KYC", CK, null));
	}

	@Test
	void listOfCellWrapsPredicateFailure() {
		ExcelCalculationException e = assertThrows(ExcelCalculationException.class,
				() -> engine.listOfCell("KYC", CK, c -> { throw new IllegalStateException("x"); }));
		assertEquals("KYC!ComputeKYC", e.getCellAddress());
	}

	@Test
	void listOfCellUnknownResourceIsNotFound() {
		assertThrows(ExcelResourceNotFoundException.class, () -> engine.listOfCell("NOPE", CK, c -> true));
	}

	@Test
	void listOfInputUnknownResourceIsNotFound() {
		assertThrows(ExcelResourceNotFoundException.class, () -> engine.listOfInput("NOPE", CK));
	}

	@Test
	void listOfInputKycUsesLabelCells() {
		List<ExcelCell> in = engine.listOfInput("KYC", CK);
		assertEquals(Set.of("ComputeKYC!A2", "ComputeKYC!A3", "ComputeKYC!A4"), addresses(in));
		assertTrue(in.stream().allMatch(c -> "@input".equals(c.getMetadata())));
	}

	@Test
	void listOfInputKycapi() {
		List<ExcelCell> in = engine.listOfInput("KYCAPI", CK);
		assertEquals(Set.of("ComputeKYC!B2", "ComputeKYC!B3", "ComputeKYC!B4"), addresses(in));
		assertEquals(Set.of("@input(PEP)", "@input(COUNTRY)", "@input(AMOUNT)"),
				in.stream().map(ExcelCell::getMetadata).collect(Collectors.toSet()));
	}

	@Test
	void listOfOutputKycapi() {
		List<ExcelCell> out = engine.listOfOutput("KYCAPI", CK);
		assertEquals(1, out.size());
		assertEquals("ComputeKYC!C6", out.get(0).getAddress());
		assertEquals("SUM(C2:C4)", out.get(0).getValue());
	}

	@ParameterizedTest
	@CsvSource({ "KYC,Nope,0" })
	void listOfAPICount(String resource, String sheet, int count) {
		assertEquals(count, engine.listOfAPI(resource, sheet).size());
	}

	@ParameterizedTest
	@NullAndEmptySource
	void listingRejectsBlankSheet(String sheet) {
		assertThrows(ExcelInvalidInputException.class, () -> engine.listOfInput("KYC", sheet));
		assertThrows(ExcelInvalidInputException.class, () -> engine.listOfOutput("KYC", sheet));
		assertThrows(ExcelInvalidInputException.class, () -> engine.listOfAPI("KYC", sheet));
		assertThrows(ExcelInvalidInputException.class, () -> engine.mapOfFormularCell("KYC", sheet));
	}

	@ParameterizedTest
	@NullAndEmptySource
	void listingRejectsBlankResource(String resource) {
		assertThrows(ExcelInvalidInputException.class, () -> engine.listOfInput(resource, CK));
		assertThrows(ExcelInvalidInputException.class, () -> engine.listOfOutput(resource, CK));
		assertThrows(ExcelInvalidInputException.class, () -> engine.mapOfAPI(resource, CK));
		assertThrows(ExcelInvalidInputException.class, () -> engine.mapOfCell(resource, CK, c -> true));
	}

	@Test
	void mapOfAPIGroupsInAndOut() {
		Map<String, List<ExcelCell>> m = engine.mapOfAPI("KYCAPI", CK);
		assertEquals(Set.of("IN", "OUT"), m.keySet());
		assertEquals(3, m.get("IN").size());
		assertEquals(1, m.get("OUT").size());
		assertEquals("ComputeKYC!C6", m.get("OUT").get(0).getAddress());
	}

	// ---------------------------------------------------------------- apiName

	@ParameterizedTest
	@CsvSource(value = {
			"@input(PEP)|PEP",
			"@input(\"X_1\")|X_1",
			"  @output(a9)  |a9" }, delimiter = '|')
	void apiNameExtractsName(String metadata, String expected) {
		assertEquals(expected, ExcelEngineImpl.apiName(metadata));
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { "@input", "note @input(X)" })
	void apiNameReturnsNullWhenNoName(String metadata) {
		assertNull(ExcelEngineImpl.apiName(metadata));
	}
}
