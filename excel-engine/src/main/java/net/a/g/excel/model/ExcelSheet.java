package net.a.g.excel.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a worksheet within an Excel workbook.
 * This class encapsulates the metadata of an Excel worksheet.
 * It extends ExcelModel to inherit HATEOAS capabilities and supports
 * JSON serialization with null value exclusion.
 *
 * @see ExcelModel
 */
@JsonInclude(value = Include.NON_NULL)
public class ExcelSheet extends ExcelModel {

    /** The name of the worksheet */
    @JsonProperty("name")
    private String name;

    /**
     * Default constructor.
     */
    public ExcelSheet() {
	}

    /**
     * Constructs an ExcelSheet with a specified name.
     * @param name the name of the worksheet
     */
    public ExcelSheet(String name) {
		this.name = name;
	}

    /**
     * Gets the worksheet name.
     * @return the name of the worksheet
     */
    public String getName() {
		return this.name;
	}

    /**
     * Sets the worksheet name.
     * @param name the name to set for the worksheet
     */
    public void setName(String name) {
		this.name = name;
	}
}
