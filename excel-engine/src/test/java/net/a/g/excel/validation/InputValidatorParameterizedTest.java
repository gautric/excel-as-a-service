package net.a.g.excel.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import net.a.g.excel.exception.ExcelInvalidInputException;

/** Boundary and table-driven cases complementing InputValidatorTest. */
public class InputValidatorParameterizedTest {

	@ParameterizedTest
	@ValueSource(strings = { "a=b", "mail@x" })
	void cellValueAccepted(String value) {
		assertDoesNotThrow(() -> InputValidator.validateCellValue(value, "A1"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "-1", "@SUM(A1)", "  =A1" })
	void cellValueFormulaInjectionRejected(String value) {
		ExcelInvalidInputException e = assertThrows(ExcelInvalidInputException.class,
				() -> InputValidator.validateCellValue(value, "B4"));
		assertEquals("B4", e.getInputName());
		assertEquals(value, e.getInputValue());
		assertTrue(e.getMessage().contains("formula injection"));
	}

	@Test
	void cellValueAtMaxLengthAccepted() {
		assertDoesNotThrow(() -> InputValidator.validateCellValue("x".repeat(32767), "A1"));
	}

	@Test
	void cellValueOverMaxLengthRejectedWithTruncatedValue() {
		ExcelInvalidInputException e = assertThrows(ExcelInvalidInputException.class,
				() -> InputValidator.validateCellValue("y".repeat(32768), "A1"));
		assertEquals("y".repeat(50) + "...", e.getInputValue());
		assertTrue(e.getMessage().contains("32767"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "XFD1048576", "Compute_KYC!C6", " A1 " })
	void cellReferenceAccepted(String ref) {
		assertDoesNotThrow(() -> InputValidator.validateCellReference(ref));
	}

	@ParameterizedTest
	@ValueSource(strings = { "A12345678", "1Sheet!A1", "A1:B2" })
	void cellReferenceRejected(String ref) {
		ExcelInvalidInputException e = assertThrows(ExcelInvalidInputException.class,
				() -> InputValidator.validateCellReference(ref));
		assertTrue(e.getMessage().startsWith("Invalid cell reference format: " + ref));
	}

	@ParameterizedTest
	@ValueSource(strings = { "   " })
	void cellReferenceBlankRejected(String ref) {
		ExcelInvalidInputException e = assertThrows(ExcelInvalidInputException.class,
				() -> InputValidator.validateCellReference(ref));
		assertEquals("Cell reference cannot be null or empty", e.getMessage());
	}

	@ParameterizedTest
	@ValueSource(strings = { "My Sheet", "abcdefghijklmnopqrstuvwxyz12345" })
	void sheetNameAccepted(String name) {
		assertDoesNotThrow(() -> InputValidator.validateSheetName(name));
	}

	@ParameterizedTest
	@ValueSource(strings = { "abcdefghijklmnopqrstuvwxyz123456", "a:b", "é" })
	void sheetNameRejected(String name) {
		ExcelInvalidInputException e = assertThrows(ExcelInvalidInputException.class,
				() -> InputValidator.validateSheetName(name));
		assertTrue(e.getMessage().startsWith("Invalid sheet name: " + name));
	}

	@Test
	void sheetNameWhitespaceOnlyRejected() {
		ExcelInvalidInputException e = assertThrows(ExcelInvalidInputException.class,
				() -> InputValidator.validateSheetName("  "));
		assertEquals("Sheet name cannot be null or empty", e.getMessage());
	}

	@Test
	void resourceNameAt255Accepted() {
		assertDoesNotThrow(() -> InputValidator.validateResourceName("r".repeat(255)));
	}

	@Test
	void resourceNameOver255Rejected() {
		assertThrows(ExcelInvalidInputException.class, () -> InputValidator.validateResourceName("r".repeat(256)));
	}

	@ParameterizedTest
	@ValueSource(strings = { "KYC.xlsx", "../etc/passwd" })
	void resourceNameRejected(String name) {
		ExcelInvalidInputException e = assertThrows(ExcelInvalidInputException.class,
				() -> InputValidator.validateResourceName(name));
		assertTrue(e.getMessage().startsWith("Invalid resource name: " + name));
	}

	@Test
	void resourceNameWhitespaceOnlyRejected() {
		ExcelInvalidInputException e = assertThrows(ExcelInvalidInputException.class,
				() -> InputValidator.validateResourceName("   "));
		assertEquals("Resource name cannot be null or empty", e.getMessage());
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "-2.5" })
	void numericAccepted(String v) {
		assertDoesNotThrow(() -> InputValidator.validateNumericValue(v, "B4"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "1,5", "1.2.3" })
	void numericRejected(String v) {
		ExcelInvalidInputException e = assertThrows(ExcelInvalidInputException.class,
				() -> InputValidator.validateNumericValue(v, "B4"));
		assertEquals("B4", e.getInputName());
		assertEquals(v, e.getInputValue());
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { " true " })
	void booleanAccepted(String v) {
		assertDoesNotThrow(() -> InputValidator.validateBooleanValue(v, "B2"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "yes", "1" })
	void booleanRejected(String v) {
		ExcelInvalidInputException e = assertThrows(ExcelInvalidInputException.class,
				() -> InputValidator.validateBooleanValue(v, "B2"));
		assertEquals("B2", e.getInputName());
		assertTrue(e.getMessage().contains("not a valid boolean"));
	}

	@ParameterizedTest
	@CsvSource(value = { "+-=@x|x", "= + x|x", "===|''" }, delimiter = '|')
	void sanitize(String in, String expected) {
		assertEquals(expected, InputValidator.sanitizeCellValue(in));
	}

	@Test
	void sanitizeNull() {
		assertNull(InputValidator.sanitizeCellValue(null));
	}

	@Test
	void sanitizedValuePassesValidation() {
		String s = InputValidator.sanitizeCellValue("=cmd|' /C calc'!A0");
		assertDoesNotThrow(() -> InputValidator.validateCellValue(s, "A1"));
	}

	@Test
	void cannotBeInstantiated() throws Exception {
		Constructor<InputValidator> c = InputValidator.class.getDeclaredConstructor();
		c.setAccessible(true);
		InvocationTargetException e = assertThrows(InvocationTargetException.class, c::newInstance);
		assertInstanceOf(UnsupportedOperationException.class, e.getCause());
	}
}
