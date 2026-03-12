package net.a.g.excel.exception;

/**
 * Exception thrown when invalid input is provided to Excel operations.
 * This includes invalid cell references, malicious formulas, or type mismatches.
 */
public class ExcelInvalidInputException extends ExcelEngineException {

    private final String inputName;
    private final String inputValue;

    /**
     * Creates a new exception for invalid input.
     *
     * @param inputName the name or reference of the invalid input
     * @param inputValue the invalid value provided
     * @param message the error message
     */
    public ExcelInvalidInputException(String inputName, String inputValue, String message) {
        super("Invalid input '" + inputName + "' with value '" + inputValue + "': " + message);
        this.inputName = inputName;
        this.inputValue = inputValue;
    }

    /**
     * Creates a new exception with just a message.
     *
     * @param message the error message
     */
    public ExcelInvalidInputException(String message) {
        super(message);
        this.inputName = null;
        this.inputValue = null;
    }

    /**
     * Gets the name of the invalid input.
     *
     * @return the input name
     */
    public String getInputName() {
        return inputName;
    }

    /**
     * Gets the invalid value that was provided.
     *
     * @return the input value
     */
    public String getInputValue() {
        return inputValue;
    }
}
