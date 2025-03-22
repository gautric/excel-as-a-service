package net.a.g.excel.model;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents an Excel workbook resource in the system.
 * This class encapsulates the metadata and binary content of an Excel workbook.
 * It extends ExcelModel to inherit HATEOAS capabilities and supports JSON
 * serialization with null value exclusion. The binary content is excluded
 * from JSON serialization.
 *
 * @see ExcelModel
 */
@JsonInclude(value = Include.NON_NULL)
public class ExcelResource extends ExcelModel {

    /** The logical name of the resource */
    @JsonProperty("name")
    private String name;

    /** The file name or path of the resource */
    @JsonProperty("file")
    private String file;

    /** The binary content of the Excel workbook */
    @JsonIgnore(value = true)
    private byte[] doc;

    /**
     * Default constructor.
     */
    public ExcelResource() {
	}

    /**
     * Constructs an ExcelResource with a specified name.
     * @param name the logical name of the resource
     */
    public ExcelResource(String name) {
		this.name = name;
	}

    /**
     * Gets the logical name of the resource.
     * @return the resource name
     */
    public String getName() {
		return name;
	}

    /**
     * Sets the logical name of the resource.
     * @param name the resource name to set
     */
    public void setName(String name) {
		this.name = name;
	}

    /**
     * Gets the file name or path of the resource.
     * @return the file name/path
     */
    public String getFile() {
		return file;
	}

    /**
     * Sets the file name or path of the resource.
     * @param file the file name/path to set
     */
    public void setFile(String file) {
		this.file = file;
	}

    /**
     * Gets the binary content of the Excel workbook.
     * @return the workbook content as a byte array
     */
    public byte[] getDoc() {
		return doc;
	}

    /**
     * Sets the binary content of the Excel workbook.
     * @param doc the workbook content as a byte array
     */
    public void setDoc(byte[] doc) {
		this.doc = doc;
	}

    /**
     * Returns a string representation of the resource.
     * Shows the name, file, and a truncated preview of the binary content.
     * @return string representation of the resource
     */
    @Override
    public String toString() {
		return "ExcelResource [name=" + name + ", file=" + file + ", doc=" + Arrays.toString(doc).substring(0, 10)
				+ ".... ]";
	}
}
