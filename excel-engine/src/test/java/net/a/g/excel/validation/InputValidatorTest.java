package net.a.g.excel.validation;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import net.a.g.excel.exception.ExcelInvalidInputException;

/**
 * Unit tests for InputValidator class.
 * Tests validation logic for cell values, references, and names.
 */
class InputValidatorTest {

    @Test
    void testValidCellValue() {
        // Valid values should not throw exceptions
        assertDoesNotThrow(() -> InputValidator.validateCellValue("Hello World", "A1"));
        assertDoesNotThrow(() -> InputValidator.validateCellValue("123", "B2"));
        assertDoesNotThrow(() -> InputValidator.validateCellValue("true", "C3"));
        assertDoesNotThrow(() -> InputValidator.validateCellValue(null, "D4")); // Null is acceptable
    }

    @Test
    void testFormulaInjectionDetection() {
        // Values starting with formula characters should throw exception
        assertThrows(ExcelInvalidInputException.class, 
            () -> InputValidator.validateCellValue("=SUM(A1:A10)", "A1"));
        assertThrows(ExcelInvalidInputException.class, 
            () -> InputValidator.validateCellValue("+1+1", "B1"));
        assertThrows(ExcelInvalidInputException.class, 
            () -> InputValidator.validateCellValue("-1", "C1"));
        assertThrows(ExcelInvalidInputException.class, 
            () -> InputValidator.validateCellValue("@SUM(1,2)", "D1"));
    }

    @Test
    void testCellValueMaxLength() {
        // Create a string longer than MAX_CELL_VALUE_LENGTH (32767)
        String longValue = "a".repeat(32768);
        assertThrows(ExcelInvalidInputException.class, 
            () -> InputValidator.validateCellValue(longValue, "A1"));
    }

    @Test
    void testValidCellReferences() {
        // Valid cell references
        assertDoesNotThrow(() -> InputValidator.validateCellReference("A1"));
        assertDoesNotThrow(() -> InputValidator.validateCellReference("Z99"));
        assertDoesNotThrow(() -> InputValidator.validateCellReference("AA100"));
        assertDoesNotThrow(() -> InputValidator.validateCellReference("Sheet1!B2"));
        assertDoesNotThrow(() -> InputValidator.validateCellReference("MySheet!XFD1048576"));
    }

    @Test
    void testInvalidCellReferences() {
        // Invalid cell references
        assertThrows(ExcelInvalidInputException.class, 
            () -> InputValidator.validateCellReference(null));
        assertThrows(ExcelInvalidInputException.class, 
            () -> InputValidator.validateCellReference(""));
        assertThrows(ExcelInvalidInputException.class, 
            () -> InputValidator.validateCellReference("A")); // Missing row number
        assertThrows(ExcelInvalidInputException.class, 
            () -> InputValidator.validateCellReference("1")); // Missing column
        assertThrows(ExcelInvalidInputException.class, 
            () -> InputValidator.validateCellReference("A1B2")); // Invalid format
    }

    @Test
    void testValidSheetNames() {
        // Valid sheet names
        assertDoesNotThrow(() -> InputValidator.validateSheetName("Sheet1"));
        assertDoesNotThrow(() -> InputValidator.validateSheetName("My_Sheet"));
        assertDoesNotThrow(() -> InputValidator.validateSheetName("Data-2024"));
        assertDoesNotThrow(() -> InputValidator.validateSheetName("Summary Report"));
    }

    @Test
    void testInvalidSheetNames() {
        // Invalid sheet names
        assertThrows(ExcelInvalidInputException.class, 
            () -> InputValidator.validateSheetName(null));
        assertThrows(ExcelInvalidInputException.class, 
            () -> InputValidator.validateSheetName(""));
        assertThrows(ExcelInvalidInputException.class, 
            () -> InputValidator.validateSheetName("Sheet:1")); // Contains colon
        assertThrows(ExcelInvalidInputException.class, 
            () -> InputValidator.validateSheetName("Sheet/1")); // Contains slash
        assertThrows(ExcelInvalidInputException.class, 
            () -> InputValidator.validateSheetName("Sheet[1]")); // Contains brackets
        
        // Too long (>31 characters)
        assertThrows(ExcelInvalidInputException.class, 
            () -> InputValidator.validateSheetName("a".repeat(32)));
    }

    @Test
    void testValidResourceNames() {
        // Valid resource names
        assertDoesNotThrow(() -> InputValidator.validateResourceName("KYC"));
        assertDoesNotThrow(() -> InputValidator.validateResourceName("report_2024"));
        assertDoesNotThrow(() -> InputValidator.validateResourceName("data-file"));
        assertDoesNotThrow(() -> InputValidator.validateResourceName("Resource123"));
    }

    @Test
    void testInvalidResourceNames() {
        // Invalid resource names
        assertThrows(ExcelInvalidInputException.class, 
            () -> InputValidator.validateResourceName(null));
        assertThrows(ExcelInvalidInputException.class, 
            () -> InputValidator.validateResourceName(""));
        assertThrows(ExcelInvalidInputException.class, 
            () -> InputValidator.validateResourceName("resource name")); // Contains space
        assertThrows(ExcelInvalidInputException.class, 
            () -> InputValidator.validateResourceName("resource/file")); // Contains slash
        
        // Too long (>255 characters)
        assertThrows(ExcelInvalidInputException.class, 
            () -> InputValidator.validateResourceName("a".repeat(256)));
    }

    @Test
    void testValidNumericValues() {
        // Valid numeric values
        assertDoesNotThrow(() -> InputValidator.validateNumericValue("123", "A1"));
        assertDoesNotThrow(() -> InputValidator.validateNumericValue("123.45", "B2"));
        assertDoesNotThrow(() -> InputValidator.validateNumericValue("-456", "C3"));
        assertDoesNotThrow(() -> InputValidator.validateNumericValue("0.001", "D4"));
        assertDoesNotThrow(() -> InputValidator.validateNumericValue(null, "E5")); // Null is acceptable
        assertDoesNotThrow(() -> InputValidator.validateNumericValue("", "F6")); // Empty is acceptable
    }

    @Test
    void testInvalidNumericValues() {
        // Invalid numeric values
        assertThrows(ExcelInvalidInputException.class, 
            () -> InputValidator.validateNumericValue("abc", "A1"));
        assertThrows(ExcelInvalidInputException.class, 
            () -> InputValidator.validateNumericValue("12.34.56", "B2"));
        assertThrows(ExcelInvalidInputException.class, 
            () -> InputValidator.validateNumericValue("123abc", "C3"));
    }

    @Test
    void testValidBooleanValues() {
        // Valid boolean values
        assertDoesNotThrow(() -> InputValidator.validateBooleanValue("true", "A1"));
        assertDoesNotThrow(() -> InputValidator.validateBooleanValue("false", "B2"));
        assertDoesNotThrow(() -> InputValidator.validateBooleanValue("TRUE", "C3"));
        assertDoesNotThrow(() -> InputValidator.validateBooleanValue("FALSE", "D4"));
        assertDoesNotThrow(() -> InputValidator.validateBooleanValue(null, "E5")); // Null is acceptable
        assertDoesNotThrow(() -> InputValidator.validateBooleanValue("", "F6")); // Empty is acceptable
    }

    @Test
    void testInvalidBooleanValues() {
        // Invalid boolean values
        assertThrows(ExcelInvalidInputException.class, 
            () -> InputValidator.validateBooleanValue("yes", "A1"));
        assertThrows(ExcelInvalidInputException.class, 
            () -> InputValidator.validateBooleanValue("1", "B2"));
        assertThrows(ExcelInvalidInputException.class, 
            () -> InputValidator.validateBooleanValue("T", "C3"));
    }

    @Test
    void testSanitizeCellValue() {
        // Sanitization should remove leading formula characters
        assertEquals("SUM(A1:A10)", InputValidator.sanitizeCellValue("=SUM(A1:A10)"));
        assertEquals("1+1", InputValidator.sanitizeCellValue("+1+1"));
        assertEquals("1", InputValidator.sanitizeCellValue("-1"));
        assertEquals("test", InputValidator.sanitizeCellValue("@test"));
        
        // Multiple leading characters should all be removed
        assertEquals("test", InputValidator.sanitizeCellValue("=+@test"));
        
        // Values without formula characters should remain unchanged
        assertEquals("Hello", InputValidator.sanitizeCellValue("Hello"));
        assertEquals("123", InputValidator.sanitizeCellValue("123"));
        
        // Null should remain null
        assertNull(InputValidator.sanitizeCellValue(null));
    }

    @Test
    void testUtilityClassCannotBeInstantiated() {
        // InputValidator should not be instantiable (utility class)
        // Reflection wraps the exception in InvocationTargetException
        var exception = assertThrows(Exception.class, () -> {
            var constructor = InputValidator.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        });
        
        // Check that the cause is UnsupportedOperationException
        assertTrue(exception.getCause() instanceof UnsupportedOperationException);
        assertEquals("Utility class cannot be instantiated", exception.getCause().getMessage());
    }
}
