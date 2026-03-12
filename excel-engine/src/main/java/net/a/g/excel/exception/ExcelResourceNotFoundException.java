package net.a.g.excel.exception;

/**
 * Exception thrown when a requested Excel resource is not found.
 * This typically occurs when attempting to access a workbook that hasn't been loaded
 * or doesn't exist in the repository.
 */
public class ExcelResourceNotFoundException extends ExcelEngineException {

    private final String resourceName;

    /**
     * Creates a new exception for a missing resource.
     *
     * @param resourceName the name of the resource that was not found
     */
    public ExcelResourceNotFoundException(String resourceName) {
        super("Excel resource not found: " + resourceName);
        this.resourceName = resourceName;
    }

    /**
     * Creates a new exception with a custom message.
     *
     * @param resourceName the name of the resource that was not found
     * @param message custom error message
     */
    public ExcelResourceNotFoundException(String resourceName, String message) {
        super(message);
        this.resourceName = resourceName;
    }

    /**
     * Gets the name of the resource that was not found.
     *
     * @return the resource name
     */
    public String getResourceName() {
        return resourceName;
    }
}
