package net.a.g.excel.param;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.a.g.excel.util.ExcelConstants;

/**
 * Implementation of the ExcelParameter interface.
 * Provides configuration settings for Excel processing operations.
 * 
 * Default configuration:
 * - Return format: MAP
 * - Date format: ISO 8601
 * - Read-only: false
 * - Formula visibility: visible
 */
public class ExcelParameterImpl implements ExcelParameter {

    /** Logger for tracking configuration and parameter-related events */
    public final static Logger LOG = LoggerFactory.getLogger(ExcelParameterImpl.class);

    /** Flag to indicate read-only mode */
    private boolean readOnly;

    /** Return format for Excel operations (default: MAP) */
    private EXCELRETURN retourFormat = EXCELRETURN.MAP;

    /** Date format string (default: ISO 8601) */
    private String formatDate = ExcelConstants.FORMAT_DATE_ISO;

    /** Flag to control formula visibility */
    private boolean hideFormular = false;

    /**
     * Gets the return format.
     * @return the configured return format
     * @deprecated Use {@link #getRetourFormat()} instead
     */
    @Deprecated
    public EXCELRETURN getFormatRetour() {
		return retourFormat;
	}

    /**
     * Gets the configured date format pattern.
     * @return the date format pattern (e.g., "yyyy-MM-dd")
     */
    public String getFormatDate() {
		return formatDate;
	}

    /**
     * Checks if read-only mode is enabled.
     * @return true if read-only mode is active, false otherwise
     */
    public boolean isReadOnly() {
		return readOnly;
	}

    /**
     * Checks if the return format is set to LIST.
     * @return true if return format is LIST, false otherwise
     */
    public boolean returnList() {
		return retourFormat == EXCELRETURN.LIST;
	}

    /**
     * Checks if the return format is set to MAP.
     * @return true if return format is MAP, false otherwise
     */
    public boolean returnMap() {
		return retourFormat == EXCELRETURN.MAP;
	}

    /**
     * Gets the current return format.
     * @return the configured return format (MAP or LIST)
     */
    public EXCELRETURN getRetourFormat() {
		return this.retourFormat;
	}

    /**
     * Checks if formulas should be hidden.
     * @return true if formulas are hidden, false if they are visible
     */
    public boolean isHideFormular() {
		return this.hideFormular;
	}

	@Override
	public String toString() {
		return "ExcelParameterImpl [readOnly=" + this.readOnly + ", retourFormat=" + this.retourFormat + ", formatDate="
				+ this.formatDate + ", hideFormular=" + this.hideFormular + "]";
	}

    /**
     * Sets the read-only mode.
     * @param readOnly true to enable read-only mode, false to disable
     */
    public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

    /**
     * Sets the return format for Excel operations.
     * @param retourFormat the return format to use (MAP or LIST)
     */
    public void setRetourFormat(EXCELRETURN retourFormat) {
		this.retourFormat = retourFormat;
	}

    /**
     * Sets the date format pattern.
     * @param formatDate the date format pattern to use (e.g., "yyyy-MM-dd")
     */
    public void setFormatDate(String formatDate) {
		this.formatDate = formatDate;
	}

    /**
     * Sets the formula visibility.
     * @param hideFormular true to hide formulas, false to show them
     */
    public void setHideFormular(boolean hideFormular) {
		this.hideFormular = hideFormular;
	}
}
