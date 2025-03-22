package net.a.g.excel.param;

/**
 * Interface for managing Excel processing configuration parameters.
 * Provides settings for controlling:
 * - Return format preferences (Map vs List)
 * - Date formatting
 * - Read-only mode
 * - Formula visibility
 */
public interface ExcelParameter {

    /**
     * Enumeration defining the possible return formats for Excel data.
     */
    public enum EXCELRETURN {
        /** Return data as a Map structure */
        MAP,
        /** Return data as a List structure */
        LIST
    }

    /**
     * Gets the configured return format.
     * @return the format setting for return values
     * @deprecated Use {@link #getRetourFormat()} instead
     */
    @Deprecated
    EXCELRETURN getFormatRetour();

    /**
     * Gets the configured date format pattern.
     * @return the date format pattern (e.g., "yyyy-MM-dd")
     */
    String getFormatDate();

    /**
     * Checks if read-only mode is enabled.
     * @return true if read-only mode is enabled, false otherwise
     */
    boolean isReadOnly();

    /**
     * Checks if list return format is configured.
     * @return true if return format is LIST, false otherwise
     */
    boolean returnList();

    /**
     * Checks if map return format is configured.
     * @return true if return format is MAP, false otherwise
     */
    boolean returnMap();

    /**
     * Gets the configured return format.
     * @return the format setting for return values
     */
    EXCELRETURN getRetourFormat();

    /**
     * Checks if formula hiding is enabled.
     * @return true if formulas should be hidden, false otherwise
     */
    boolean isHideFormular();

    /**
     * Returns a string representation of the parameters.
     * @return string containing parameter values
     */
    String toString();

    /**
     * Sets the read-only mode.
     * @param readOnly true to enable read-only mode, false to disable
     */
    void setReadOnly(boolean readOnly);

    /**
     * Sets the return format.
     * @param retourFormat the format to use for return values
     */
    void setRetourFormat(EXCELRETURN retourFormat);

    /**
     * Sets the date format pattern.
     * @param formatDate the date format pattern to use (e.g., "yyyy-MM-dd")
     */
    void setFormatDate(String formatDate);

    /**
     * Sets formula hiding mode.
     * @param hideFormular true to hide formulas, false to show them
     */
    void setHideFormular(boolean hideFormular);

}
