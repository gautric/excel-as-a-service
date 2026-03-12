package net.a.g.excel.validation;

import java.util.regex.Pattern;

import net.a.g.excel.exception.ExcelInvalidInputException;

/**
 * Utility class for validating user inputs to prevent injection attacks
 * and ensure data integrity in Excel operations.
 */
public class InputValidator {

    // Pattern to detect potential formula injection attempts
    private static final Pattern FORMULA_INJECTION_PATTERN = Pattern.compile("^[=+\\-@].*");
    
    // Pattern for valid cell references (e.g., A1, Sheet1!B2)
    private static final Pattern CELL_REFERENCE_PATTERN = Pattern.compile(
        "^([a-zA-Z][a-zA-Z0-9_]*!)?[A-Z]{1,3}[0-9]{1,7}$"
    );
    
    // Pattern for valid sheet names
    private static final Pattern SHEET_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-\\s]{1,31}$");
    
    // Pattern for valid resource names
    private static final Pattern RESOURCE_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-]{1,255}$");
    
    // Maximum allowed string length for cell values
    private static final int MAX_CELL_VALUE_LENGTH = 32767; // Excel cell limit

    /**
     * Private constructor to prevent instantiation.
     */
    private InputValidator() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Validates a cell value input to prevent formula injection attacks.
     * 
     * @param value the cell value to validate
     * @param cellReference the cell reference for error reporting
     * @throws ExcelInvalidInputException if the value contains potential injection
     */
    public static void validateCellValue(String value, String cellReference) {
        if (value == null) {
            return; // Null values are acceptable
        }

        // Check length
        if (value.length() > MAX_CELL_VALUE_LENGTH) {
            throw new ExcelInvalidInputException(
                cellReference, 
                value.substring(0, 50) + "...", 
                "Value exceeds maximum length of " + MAX_CELL_VALUE_LENGTH + " characters"
            );
        }

        // Check for formula injection
        if (FORMULA_INJECTION_PATTERN.matcher(value.trim()).matches()) {
            throw new ExcelInvalidInputException(
                cellReference,
                value,
                "Potential formula injection detected. Values cannot start with =, +, -, or @"
            );
        }
    }

    /**
     * Validates a cell reference format.
     * 
     * @param cellReference the cell reference to validate (e.g., "A1" or "Sheet1!B2")
     * @throws ExcelInvalidInputException if the reference is invalid
     */
    public static void validateCellReference(String cellReference) {
        if (cellReference == null || cellReference.trim().isEmpty()) {
            throw new ExcelInvalidInputException("Cell reference cannot be null or empty");
        }

        if (!CELL_REFERENCE_PATTERN.matcher(cellReference.trim()).matches()) {
            throw new ExcelInvalidInputException(
                "Invalid cell reference format: " + cellReference + 
                ". Expected format: 'A1' or 'Sheet1!A1'"
            );
        }
    }

    /**
     * Validates a sheet name.
     * 
     * @param sheetName the sheet name to validate
     * @throws ExcelInvalidInputException if the sheet name is invalid
     */
    public static void validateSheetName(String sheetName) {
        if (sheetName == null || sheetName.trim().isEmpty()) {
            throw new ExcelInvalidInputException("Sheet name cannot be null or empty");
        }

        if (!SHEET_NAME_PATTERN.matcher(sheetName).matches()) {
            throw new ExcelInvalidInputException(
                "Invalid sheet name: " + sheetName + 
                ". Sheet names must be 1-31 characters and contain only letters, numbers, spaces, hyphens, and underscores"
            );
        }

        // Check for invalid characters that could cause issues
        if (sheetName.contains(":") || sheetName.contains("\\") || 
            sheetName.contains("/") || sheetName.contains("?") || 
            sheetName.contains("*") || sheetName.contains("[") || 
            sheetName.contains("]")) {
            throw new ExcelInvalidInputException(
                "Sheet name contains invalid characters: " + sheetName
            );
        }
    }

    /**
     * Validates a resource name.
     * 
     * @param resourceName the resource name to validate
     * @throws ExcelInvalidInputException if the resource name is invalid
     */
    public static void validateResourceName(String resourceName) {
        if (resourceName == null || resourceName.trim().isEmpty()) {
            throw new ExcelInvalidInputException("Resource name cannot be null or empty");
        }

        if (!RESOURCE_NAME_PATTERN.matcher(resourceName).matches()) {
            throw new ExcelInvalidInputException(
                "Invalid resource name: " + resourceName + 
                ". Resource names must be 1-255 characters and contain only letters, numbers, hyphens, and underscores"
            );
        }
    }

    /**
     * Validates a numeric value string.
     * 
     * @param value the value to validate
     * @param cellReference the cell reference for error reporting
     * @throws ExcelInvalidInputException if the value is not a valid number
     */
    public static void validateNumericValue(String value, String cellReference) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }

        try {
            Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new ExcelInvalidInputException(
                cellReference,
                value,
                "Value is not a valid number"
            );
        }
    }

    /**
     * Validates a boolean value string.
     * 
     * @param value the value to validate
     * @param cellReference the cell reference for error reporting
     * @throws ExcelInvalidInputException if the value is not a valid boolean
     */
    public static void validateBooleanValue(String value, String cellReference) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }

        String normalizedValue = value.trim().toLowerCase();
        if (!normalizedValue.equals("true") && !normalizedValue.equals("false")) {
            throw new ExcelInvalidInputException(
                cellReference,
                value,
                "Value is not a valid boolean (true/false)"
            );
        }
    }

    /**
     * Sanitizes a cell value by removing potentially dangerous characters.
     * This is a safety measure but validation should be the primary defense.
     * 
     * @param value the value to sanitize
     * @return the sanitized value
     */
    public static String sanitizeCellValue(String value) {
        if (value == null) {
            return null;
        }

        // Remove leading characters that could start a formula
        String sanitized = value.trim();
        while (!sanitized.isEmpty() && "=+-@".indexOf(sanitized.charAt(0)) >= 0) {
            sanitized = sanitized.substring(1).trim();
        }

        return sanitized;
    }
}
