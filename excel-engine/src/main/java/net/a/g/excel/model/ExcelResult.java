package net.a.g.excel.model;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import net.a.g.excel.util.ExcelConstants;

/**
 * Represents the result of Excel operations.
 * This class encapsulates various types of results including:
 * - Cell values and calculations
 * - Resource listings
 * - Sheet information
 * - Error details
 * 
 * It extends ExcelModel to inherit HATEOAS capabilities and supports JSON
 * serialization with null value exclusion and OpenAPI documentation.
 *
 * @see ExcelModel
 * @see ExcelCell
 * @see ExcelResource
 * @see ExcelSheet
 */
@JsonInclude(value = Include.NON_NULL)
@Schema(name = "ExcelResult", description = "Result container for Excel operations including cells, resources, and sheets.")
public class ExcelResult extends ExcelModel {

    /** The unique identifier matching the original request */
    @JsonProperty(value = "uuid", namespace = ExcelConstants.SCHEMA_URI)
    @Schema(name = "uuid", description = "UUID of the corresponding request")
    private String uuid;

    /** The number of items in the result */
    @Schema(required = true, description = "Number of items in the result")
    @JsonProperty(value = "_count", namespace = ExcelConstants.SCHEMA_URI)
    private int count;

    /** Collection of cell results */
    @JsonProperty(value = "cells", namespace = ExcelConstants.SCHEMA_URI)
    @Schema(oneOf = { ExcelCell[].class }, description = "Collection of cell results from calculations")
    private Collection<ExcelCell> cells;

    /** Collection of Excel resources */
    @JsonProperty(value = "resources", namespace = ExcelConstants.SCHEMA_URI)
    @Schema(oneOf = { ExcelResource[].class }, description = "Collection of Excel resources")
    private Collection<ExcelResource> resources;

    /** Collection of Excel sheets */
    @JsonProperty(value = "sheets", namespace = ExcelConstants.SCHEMA_URI)
    @Schema(oneOf = { ExcelSheet[].class }, description = "Collection of Excel sheets")
    private Collection<ExcelSheet> sheets;

    /** Error information if operation failed */
    @JsonProperty(required = false, value = "error", namespace = ExcelConstants.SCHEMA_URI)
    private ExcelError error;

    /**
     * Default constructor.
     */
    public ExcelResult() {
	}

    /**
     * Constructs an ExcelResult from a collection of model objects.
     * Automatically determines the type of results (cells, sheets, or resources)
     * based on the collection contents and populates the appropriate field.
     *
     * @param results collection of ExcelModel objects to include in the result
     */
    public ExcelResult(Collection<? extends ExcelModel> results) {
		this.count = results.size();
		if (results.size() > 0 && results.iterator().next() instanceof ExcelSheet) {
			this.sheets = results.stream().map(e -> (ExcelSheet) e).collect(Collectors.toList());

		} else if (results.size() > 0 && results.iterator().next() instanceof ExcelCell) {
			this.cells = results.stream().map(e -> (ExcelCell) e).collect(Collectors.toList());

		} else {
			this.resources = results.stream().map(e -> (ExcelResource) e).collect(Collectors.toList());

		}
	}

    /**
     * Gets the UUID of the result.
     * @return the unique identifier matching the original request
     */
    public String getUuid() {
		return this.uuid;
	}

    /**
     * Sets the UUID of the result.
     * @param uuid the unique identifier to set
     */
    public void setUuid(String uuid) {
		this.uuid = uuid;
	}

    /**
     * Gets the error information if operation failed.
     * @return the error details, or null if operation succeeded
     */
    public ExcelError getError() {
		return error;
	}

    /**
     * Sets the error information.
     * @param error the error details to set
     */
    public void setError(ExcelError error) {
		this.error = error;
	}

    /**
     * Gets the number of items in the result.
     * @return the count of result items
     */
    public int getCount() {
		return count;
	}

    /**
     * Sets the number of items in the result.
     * @param count the count to set
     */
    public void setCount(int count) {
		this.count = count;
	}

    /**
     * Gets the collection of cell results.
     * @return collection of calculated cell values
     */
    public Collection<ExcelCell> getCells() {
		return this.cells;
	}

    /**
     * Sets the collection of cell results.
     * @param cells collection of cell values to set
     */
    public void setCells(Collection<ExcelCell> cells) {
		this.cells = cells;
	}

    /**
     * Gets the collection of Excel resources.
     * @return collection of Excel resources
     */
    public Collection<ExcelResource> getResources() {
		return this.resources;
	}

    /**
     * Sets the collection of Excel resources.
     * @param resources collection of resources to set
     */
    public void setResources(Collection<ExcelResource> resources) {
		this.resources = resources;
	}

    /**
     * Gets the collection of Excel sheets.
     * @return collection of Excel sheets
     */
    public Collection<ExcelSheet> getSheets() {
		return this.sheets;
	}

    /**
     * Sets the collection of Excel sheets.
     * @param sheets collection of sheets to set
     */
    public void setSheets(List<ExcelSheet> sheets) {
		this.sheets = sheets;
	}
}
