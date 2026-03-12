package net.a.g.excel.exception;

/**
 * Exception thrown when a cell calculation or formula evaluation fails.
 * This can occur due to invalid formulas, circular references, or unsupported functions.
 */
public class ExcelCalculationException extends ExcelEngineException {

    private final String cellAddress;

    /**
     * Creates a new exception for a calculation error.
     *
     * @param cellAddress the address of the cell that failed calculation
     * @param message the error message
     */
    public ExcelCalculationException(String cellAddress, String message) {
        super("Calculation error at " + cellAddress + ": " + message);
        this.cellAddress = cellAddress;
    }

    /**
     * Creates a new exception with a cause.
     *
     * @param cellAddress the address of the cell that failed calculation
     * @param message the error message
     * @param cause the underlying cause
     */
    public ExcelCalculationException(String cellAddress, String message, Throwable cause) {
        super("Calculation error at " + cellAddress + ": " + message, cause);
        this.cellAddress = cellAddress;
    }

    /**
     * Gets the address of the cell that failed calculation.
     *
     * @return the cell address
     */
    public String getCellAddress() {
        return cellAddress;
    }
}
