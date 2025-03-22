package net.a.g.excel.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Represents an error that can occur during Excel operations.
 * This class encapsulates error information including an error code
 * and error message. It supports JSON serialization with null value exclusion.
 */
@JsonInclude(value = Include.NON_NULL)
public class ExcelError {

    /** The error code identifying the type of error */
    @JsonProperty("code")
    private String code;
    
    /** The error message providing details about what went wrong */
    @JsonProperty("message")
    private String error;

    /**
     * Gets the error code.
     * @return the error code
     */
    public String getCode() {
		return code;
	}

    /**
     * Sets the error code.
     * @param code the error code to set
     */
    public void setCode(String code) {
		this.code = code;
	}

    /**
     * Gets the error message.
     * @return the error message
     */
    public String getError() {
		return error;
	}

    /**
     * Sets the error message.
     * @param error the error message to set
     */
    public void setError(String error) {
		this.error = error;
	}

}
