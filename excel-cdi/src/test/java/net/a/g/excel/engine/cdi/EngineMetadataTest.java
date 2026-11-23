package net.a.g.excel.engine.cdi;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.CellType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import net.a.g.excel.exception.ExcelInvalidInputException;
import net.a.g.excel.exception.ExcelResourceNotFoundException;
import net.a.g.excel.model.ExcelCell;
import net.a.g.excel.model.ExcelSheet;

@DisplayName("Engine introspection: sheets, cells and API annotations")
public class EngineMetadataTest extends CdiTestSupport {

	@BeforeEach
	void loadWorkbooks() {
		load("KYC", "KYCAPI.xlsx");
		load("KYCOLD", "KYC.xlsx");
		load("Type", "Type.xlsx");
		load("Test1", "Test1.xlsx");
		load("Primary", "Primary.xlsx");
	}

	@ParameterizedTest(name = "{0} has sheets {1}")
	@CsvSource(delimiter = ';', value = { "KYC;ComputeKYC,COUNTRY,AMOUNT",
			"Test1;Sheet1,Sheet2,Sheet3", "Primary;Feuil1" })
	void listOfSheetKeepsWorkbookOrder(String resource, String sheets) {
		List<String> actual = engine.listOfSheet(resource).stream().map(ExcelSheet::getName).collect(toList());
		assertThat(actual, is(Arrays.asList(sheets.split(","))));
	}

	@Test
	void listOfSheetOfUnknownResourceIsTyped() {
		ExcelResourceNotFoundException ex = assertThrows(ExcelResourceNotFoundException.class,
				() -> engine.listOfSheet("Missing"));
		assertEquals("Missing", ex.getResourceName());
	}

	@ParameterizedTest(name = "resource name ''{0}''")
	@NullAndEmptySource
	@ValueSource(strings = { "KYC API" })
	void invalidResourceNamesAreRejectedBeforeLookup(String name) {
		assertThrows(ExcelInvalidInputException.class, () -> engine.listOfSheet(name));
	}

	@Test
	void getSheetReturnsExistingSheet() {
		assertEquals("COUNTRY", engine.getSheet("KYC", "COUNTRY").getName());
	}

	@Test
	void getSheetOfUnknownSheetIsTyped() {
		assertThrows(ExcelResourceNotFoundException.class, () -> engine.getSheet("KYC", "Nope"));
	}

	@ParameterizedTest(name = "isSheetExists({0},{1}) = {2}")
	@CsvSource(value = { "KYC,AMOUNT,true", "KYC,Sheet1,false", "Missing,ComputeKYC,false",
			"NULL,ComputeKYC,false", "KYC,' ',false" }, nullValues = "NULL")
	void isSheetExistsNeverThrows(String resource, String sheet, boolean expected) {
		assertEquals(expected, engine.isSheetExists(resource, sheet));
	}

	@ParameterizedTest(name = "sheet name ''{0}''")
	@ValueSource(strings = { "Sheet[1]", "this-sheet-name-is-longer-than-31-chars" })
	void invalidSheetNamesAreRejected(String sheet) {
		assertThrows(ExcelInvalidInputException.class, () -> engine.mapOfFormularCell("KYC", sheet));
	}

	@Test
	void mapOfAPISplitsInputsAndOutputs() {
		Map<String, List<ExcelCell>> api = engine.mapOfAPI("KYC", "ComputeKYC");
		assertThat(api.keySet(), containsInAnyOrder("IN", "OUT"));
		assertThat(api.get("IN").stream().map(ExcelCell::getAddress).collect(toList()),
				contains("ComputeKYC!B2", "ComputeKYC!B3", "ComputeKYC!B4"));
		assertThat(api.get("OUT").stream().map(ExcelCell::getAddress).collect(toList()), contains("ComputeKYC!C6"));
	}

	@Test
	void inputCellsCarryTheirCurrentTypedValue() {
		List<ExcelCell> in = engine.listOfInput("KYC", "ComputeKYC");
		assertThat(in.stream().map(ExcelCell::getType).collect(toList()), contains("BOOLEAN", "STRING", "NUMERIC"));
		assertThat(in.stream().map(ExcelCell::getValue).collect(toList()), contains("false", "FR", 0.0));
	}

	@Test
	void sheetWithoutAnnotationsHasNoAPI() {
		assertThat(engine.listOfAPI("KYC", "COUNTRY"), empty());
		assertThat(engine.mapOfAPI("Test1", "Sheet1"), anEmptyMap());
	}

	@Test
	void unnamedAnnotationsStillCountAsAPICells() {
		// KYC.xlsx annotates labels with a bare "@input" / "@output" (no name)
		assertThat(engine.listOfInput("KYCOLD", "ComputeKYC").stream().map(ExcelCell::getAddress).collect(toList()),
				contains("ComputeKYC!A2", "ComputeKYC!A3", "ComputeKYC!A4"));
		assertThat(engine.listOfOutput("KYCOLD", "ComputeKYC"), hasSize(1));
	}

	@Test
	void mapOfFormularCellOnLookupSheet() {
		Map<String, ExcelCell> f = engine.mapOfFormularCell("KYC", "AMOUNT");
		assertThat(f.keySet(), containsInAnyOrder("A3", "A4", "A5"));
		assertEquals("A2*10", f.get("A3").getValue());
		assertThat(f.values().stream().map(ExcelCell::getType).collect(toList()), everyItem(is("FORMULA")));
	}

	@Test
	void listOfCellWithCustomPredicate() {
		List<ExcelCell> numeric = engine.listOfCell("KYC", "AMOUNT", c -> c.getCellType() == CellType.NUMERIC);
		assertThat(numeric, hasSize(7));
		assertThat(numeric.stream().map(ExcelCell::getAddress).collect(toList()), everyItem(startsWith("AMOUNT!")));
		assertThat(engine.mapOfCell("KYC", "COUNTRY", c -> c.getCellType() == CellType.STRING).keySet(),
				containsInAnyOrder("A1", "A2", "A3", "A4", "A5"));
	}

	@Test
	void nullPredicateIsRejected() {
		assertThrows(IllegalArgumentException.class, () -> engine.listOfCell("KYC", "AMOUNT", null));
		assertThrows(IllegalArgumentException.class, () -> engine.mapOfCell("KYC", "AMOUNT", null));
	}

	@Test
	void cellListingOfUnknownResourceIsNotFound() {
		ExcelResourceNotFoundException e = assertThrows(ExcelResourceNotFoundException.class,
				() -> engine.listOfInput("Missing", "ComputeKYC"));
		assertEquals("Missing", e.getResourceName());
	}

	@Test
	void cellListingOfUnknownSheetIsEmpty() {
		assertThat(engine.listOfAPI("KYC", "Nope"), empty());
		assertThat(engine.mapOfFormularCell("KYC", "Nope"), anEmptyMap());
	}

	@Test
	void sheetNameCaseIsHandledConsistently() {
		assertEquals(true, engine.isSheetExists("KYC", "computekyc"));
		assertThat(engine.listOfAPI("KYC", "computekyc"), hasSize(4));
	}

	@ParameterizedTest(name = "apiName(''{0}'') = {1}")
	@CsvSource(value = { "'@output(\"SCORE\")',SCORE", "'  @input(AMOUNT_2)  ',AMOUNT_2", "@input,NULL",
			"@input(A B),NULL", "NULL,NULL" }, nullValues = "NULL")
	void apiNameExtraction(String metadata, String expected) {
		assertEquals(expected, ExcelEngineImpl.apiName(metadata));
	}
}
