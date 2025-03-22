package net.a.g.excel.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a cell in an Excel workbook.
 * This class encapsulates all the properties of an Excel cell including its address,
 * value, metadata, and type. It supports JSON serialization with null value exclusion.
 *
 * @see ExcelModel
 */
@JsonInclude(value = Include.NON_NULL)
public class ExcelCell extends ExcelModel {

    /** The cell's address in A1 notation (e.g., "A1", "B2") */
    @JsonProperty("address")
    private String address;

    /** The cell's value, which can be a string, number, boolean, or other Excel-supported types */
    @JsonProperty("value")
    private Object value;

    /** Additional metadata associated with the cell (e.g., comments, annotations) */
    @JsonProperty("metadata")
    private String metadata;

    /** The cell's data type (e.g., "STRING", "NUMERIC", "BOOLEAN", "FORMULA") */
    @JsonProperty("type")
    private String type;

    /**
     * Gets the cell's address.
     * @return the cell address in A1 notation
     */
    public String getAddress() {
		return address;
	}

    /**
     * Sets the cell's address.
     * @param address the cell address in A1 notation
     */
    public void setAddress(String address) {
		this.address = address;
	}

    /**
     * Gets the cell's value.
     * @return the cell value as an Object
     */
    public Object getValue() {
		return value;
	}

    /**
     * Sets the cell's value.
     * @param value the value to set
     */
    public void setValue(Object value) {
		this.value = value;
	}

    /**
     * Gets the cell's metadata.
     * @return the metadata string
     */
    public String getMetadata() {
		return metadata;
	}

    /**
     * Sets the cell's metadata.
     * @param metadata the metadata to set
     */
    public void setMetadata(String metadata) {
		this.metadata = metadata;
	}

    /**
     * Gets the cell's data type.
     * @return the type string
     */
    public String getType() {
		return type;
	}

    /**
     * Sets the cell's data type.
     * @param type the type to set
     */
    public void setType(String type) {
		this.type = type;
	}

    /**
     * Default constructor.
     */
    public ExcelCell() {
	};

    /**
     * Constructs an ExcelCell with specified properties.
     *
     * @param adress the cell address
     * @param formula the cell formula (unused)
     * @param value the cell value
     * @param ref reference information (unused)
     * @param metadata cell metadata (unused)
     */
    public ExcelCell(String adress, String formula, Object value, String ref, String metadata) {
		this.address = adress;
		this.value = value;
	}

	@Override
	public String toString() {
		return "ExcelCell [address=" + this.address + ", value=" + this.value + ", metadata=" + this.metadata
				+ ", type=" + this.type + "]";
	};

}
