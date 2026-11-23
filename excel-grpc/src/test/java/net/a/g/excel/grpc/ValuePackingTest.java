package net.a.g.excel.grpc;

import static net.a.g.excel.grpc.Fixtures.*;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.google.protobuf.BoolValue;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.StringValue;

import net.a.g.excel.repository.ExcelRepositoryImpl;

/** Any packing: DoubleValue for numbers, BoolValue for BOOLEAN, StringValue otherwise. */
class ValuePackingTest {

	static ExcelService service;
	static Map<String, ExcelCell> typeSheet;

	@BeforeAll
	static void setup() {
		ExcelRepositoryImpl repo = repo("Type", "KYCAPI");
		repo.add(resource("Synthetic", "Synthetic.xlsx", bytes(synthetic())));
		service = service(repo);
		typeSheet = items(service.listOfCell(req("Type", "Sheet1").build())).stream()
				.collect(Collectors.toMap(ExcelCell::getAddress, c -> c));
	}

	/** A workbook with the cell kinds the samples lack: boolean formulas, errors, text formulas. */
	private static XSSFWorkbook synthetic() {
		XSSFWorkbook wb = new XSSFWorkbook();
		Sheet s = wb.createSheet("S");
		Row r = s.createRow(0);
		r.createCell(0).setCellValue(3);
		r.createCell(1).setCellFormula("A1>2");
		r.createCell(2).setCellFormula("1/0");
		r.createCell(3).setCellFormula("\"n=\"&A1");
		r.createCell(4).setCellValue(true);
		r.createCell(5).setCellFormula("NOT(E1)");
		r.createCell(6).setCellValue(-0.5);
		r.createCell(7).setCellFormula("A1/4");
		return wb;
	}

	private static ExcelCell compute(String resource, String sheet, String out) {
		return single(service.computeCell(req(resource, sheet).addOutputs(out).build()));
	}

	// --- raw values from listOfCell (Type.xlsx) ----------------------------

	@ParameterizedTest(name = "{0} {1} -> {2}")
	@CsvSource({ "Sheet1!A1,NUMERIC,0", "Sheet1!A2,NUMERIC,1", "Sheet1!A6,NUMERIC,123" })
	void numericCellsArePackedAsDoubleValue(String address, String type, double expected) {
		ExcelCell c = typeSheet.get(address);
		assertEquals(type, c.getType());
		assertEquals(expected, dbl(c));
	}

	@ParameterizedTest(name = "{0} {1} -> \"{2}\"")
	@CsvSource(delimiter = ';', ignoreLeadingAndTrailingWhitespace = false, value = { "Sheet1!B1;STRING;--",
			"Sheet1!A3;STRING;A", "Sheet1!A4;STRING;B", "Sheet1!A7;STRING;Hello ", "Sheet1!A5;DATE;2021-01-01",
			"Sheet1!C3;BLANK;", "Sheet1!A8;FORMULA;A1:A2", "Sheet1!A9;FORMULA;A1+A2+41" })
	void nonNumericCellsArePackedAsStringValue(String address, String type, String expected) {
		ExcelCell c = typeSheet.get(address);
		assertEquals(type, c.getType());
		assertEquals(expected == null ? "" : expected, str(c));
	}

	@Test
	void dateIsAStringInIsoFormatNotASerialNumber() {
		assertFalse(typeSheet.get("Sheet1!A5").getValue().is(DoubleValue.class));
	}

	@Test
	void booleanCellIsPackedAsBoolValue() {
		ExcelCell b2 = items(service.listOfCell(req("KYCAPI", "ComputeKYC").build())).stream()
				.filter(c -> c.getAddress().equals("ComputeKYC!B2")).findFirst().orElseThrow();
		assertEquals("BOOLEAN", b2.getType());
		assertFalse(bool(b2));
	}

	@Test
	void anyTypeUrlIsTheStandardWrapperType() {
		assertEquals("type.googleapis.com/google.protobuf.DoubleValue", typeSheet.get("Sheet1!A1").getValue().getTypeUrl());
		assertEquals("type.googleapis.com/google.protobuf.StringValue", typeSheet.get("Sheet1!A3").getValue().getTypeUrl());
	}

	// --- evaluated values from computeCell --------------------------------

	@Test
	void evaluatedNumericFormulaBecomesNumericDouble() {
		ExcelCell c = compute("Type", "Sheet1", "A9");
		assertEquals("NUMERIC", c.getType());
		assertEquals(42.0, dbl(c));
	}

	@Test
	void evaluatedDateCellStaysAnIsoString() {
		ExcelCell c = compute("Type", "Sheet1", "A5");
		assertEquals("DATE", c.getType());
		assertEquals("2021-01-01", str(c));
	}

	@ParameterizedTest(name = "date input \"{0}\"")
	@CsvSource({ "2024-02-29", "1999-12-31" })
	void dateInputRoundTrips(String date) {
		ExcelCell c = single(service.computeCell(req("Type", "Sheet1").addOutputs("A5").putInputs("A5", date).build()));
		assertEquals(date, str(c));
	}

	@Test
	void malformedDateInputIsInvalidArgument() {
		failure(service.computeCell(req("Type", "Sheet1").addOutputs("A5").putInputs("A5", "01/02/2024").build()),
				io.grpc.Status.Code.INVALID_ARGUMENT);
	}

	@ParameterizedTest(name = "S!{0} -> {1}")
	@CsvSource({ "B1,BOOLEAN,true", "E1,BOOLEAN,true", "F1,BOOLEAN,false" })
	void booleanResultsArePackedAsBoolValue(String out, String type, boolean expected) {
		ExcelCell c = compute("Synthetic", "S", out);
		assertEquals(type, c.getType());
		assertEquals(expected, bool(c));
	}

	@Test
	void booleanFormulaReactsToInput() {
		ExcelCell c = single(service.computeCell(req("Synthetic", "S").addOutputs("B1").putInputs("A1", "1").build()));
		assertFalse(bool(c));
	}

	@Test
	void errorFormulaIsPackedAsErrorString() {
		ExcelCell c = compute("Synthetic", "S", "C1");
		assertEquals("ERROR", c.getType());
		assertEquals("#ERROR", str(c));
	}

	@Test
	void textFormulaIsPackedAsStringValue() {
		ExcelCell c = compute("Synthetic", "S", "D1");
		assertEquals("STRING", c.getType());
		assertEquals("n=3", str(c));
	}

	@ParameterizedTest(name = "S!{0} -> {1}")
	@CsvSource({ "G1,-0.5", "H1,0.75" })
	void fractionalAndNegativeNumbersKeepFullPrecision(String out, double expected) {
		assertEquals(expected, dbl(compute("Synthetic", "S", out)));
	}

	@Test
	void evaluatedRangeFormulaIsAnErrorString() {
		ExcelCell c = compute("Type", "Sheet1", "A8");
		assertEquals("ERROR", c.getType());
		assertEquals("#ERROR", str(c));
	}

	// --- functorMap on model cells ---------------------------------------

	private static net.a.g.excel.model.ExcelCell model(String type, Object value) {
		net.a.g.excel.model.ExcelCell c = new net.a.g.excel.model.ExcelCell();
		c.setAddress("S!A1");
		c.setType(type);
		c.setValue(value);
		return c;
	}

	@Test
	void nullValueLeavesTheValueFieldUnset() {
		ExcelCell c = ExcelService.functorMap.apply(model("BLANK", null));
		assertFalse(c.hasValue());
		assertEquals("S!A1", c.getAddress());
	}

	@Test
	void nullAddressTypeAndMetadataLeaveTheirFieldsUnset() {
		ExcelCell c = ExcelService.functorMap.apply(new net.a.g.excel.model.ExcelCell());
		assertFalse(c.hasAddress());
		assertFalse(c.hasType());
		assertFalse(c.hasMetadata());
		assertFalse(c.hasValue());
	}

	@Test
	void anyJavaNumberIsWidenedToDouble() {
		assertEquals(7.0, dbl(ExcelService.functorMap.apply(model("NUMERIC", 7))));
		assertEquals(9.0E15, dbl(ExcelService.functorMap.apply(model("NUMERIC", 9_000_000_000_000_000L))));
		assertEquals(1.25, dbl(ExcelService.functorMap.apply(model("NUMERIC", new BigDecimal("1.25")))));
	}

	@ParameterizedTest(name = "BOOLEAN \"{0}\" -> {1}")
	@CsvSource({ "true,true", "TRUE,true", "false,false", "yes,false" })
	void booleanTypeUsesBooleanParsing(String raw, boolean expected) {
		ExcelCell c = ExcelService.functorMap.apply(model("BOOLEAN", raw));
		assertEquals(expected, unpack(c, BoolValue.class).getValue());
	}

	@Test
	void numberWinsOverTheBooleanType() {
		assertTrue(ExcelService.functorMap.apply(model("BOOLEAN", 1.0)).getValue().is(DoubleValue.class));
	}

	@Test
	void booleanTextInAStringCellStaysAString() {
		assertEquals("true", unpack(ExcelService.functorMap.apply(model("STRING", "true")), StringValue.class).getValue());
	}
}
