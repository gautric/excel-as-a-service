package net.a.g.excel.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a request for Excel operations.
 * This class encapsulates all parameters needed to perform Excel calculations,
 * including resource identification, input values, and output cell specifications.
 * Each request is uniquely identified by a UUID and supports JSON serialization
 * with null value exclusion.
 */
@JsonInclude(value = Include.NON_NULL)
@Schema(name = "ExcelRequest", description = "Request model for Excel operations containing resource, inputs, and outputs specifications.")
public class ExcelRequest {

    /** Unique identifier for the request */
    @JsonProperty("uuid")
    @Schema(name = "uuid", description = "Unique identifier for the request")
    private String uuid;

    /** Name of the Excel resource to process */
    @JsonProperty("resource")
    @Schema(name = "resource", description = "Name of the Excel resource to process")
    private String resource;

    /** Name of the sheet to operate on */
    @JsonProperty("sheet")
    @Schema(name = "sheet", description = "Name of the sheet to process")
    private String sheet;

    /** Flag to force recalculation of formulas */
    @JsonProperty("force")
    @Schema(name = "force", description = "Whether to force formula recalculation")
    private boolean force;

    /** List of cell addresses to calculate */
    @JsonProperty("outputs")
    @Schema(name = "outputs", description = "List of cell addresses to calculate")
    private List<String> outputs = new ArrayList<String>();

    /** Map of cell addresses to their input values */
    @JsonProperty("inputs")
    @Schema(name = "inputs", description = "Map of cell addresses to their input values")
    private Map<String, String> inputs = new HashMap<String, String>();

    /**
     * Default constructor.
     * Initializes a new request with a randomly generated UUID.
     */
    public ExcelRequest() {
		uuid = UUID.randomUUID().toString();
	}

    /**
     * Constructor with specified UUID.
     * @param uuid the unique identifier for the request
     */
    public ExcelRequest(String uuid) {
		this.uuid = uuid;
	}

    /**
     * Gets the request's UUID.
     * @return the unique identifier
     */
    public String getUuid() {
		return this.uuid;
	}

    /**
     * Sets the request's UUID.
     * @param uuid the unique identifier to set
     */
    public void setUuid(String uuid) {
		this.uuid = uuid;
	}

    /**
     * Gets the Excel resource name.
     * @return the resource name
     */
    public String getResource() {
		return this.resource;
	}

    /**
     * Sets the Excel resource name.
     * @param resource the resource name to set
     */
    public void setResource(String resource) {
		this.resource = resource;
	}

    /**
     * Gets the sheet name.
     * @return the sheet name
     */
    public String getSheet() {
		return this.sheet;
	}

    /**
     * Sets the sheet name.
     * @param sheet the sheet name to set
     */
    public void setSheet(String sheet) {
		this.sheet = sheet;
	}

    /**
     * Gets the list of output cell addresses.
     * @return list of cell addresses to calculate
     */
    public List<String> getOutputs() {
		return this.outputs;
	}

    /**
     * Sets the list of output cell addresses.
     * @param outputs list of cell addresses to calculate
     */
    public void setOutputs(List<String> outputs) {
		this.outputs = outputs;
	}

    /**
     * Gets the map of input values.
     * @return map of cell addresses to their values
     */
    public Map<String, String> getInputs() {
		return this.inputs;
	}

    /**
     * Sets the map of input values.
     * @param inputs map of cell addresses to their values
     */
    public void setInputs(Map<String, String> inputs) {
		this.inputs = inputs;
	}

    /**
     * Checks if force recalculation is enabled.
     * @return true if formulas should be forcibly recalculated
     */
    public boolean isForce() {
		return force;
	}

    /**
     * Sets the force recalculation flag.
     * @param force true to force formula recalculation
     */
    public void setForce(boolean force) {
		this.force = force;
	}

	@Override
	public String toString() {
		return "ExcelRequest [uuid=" + uuid + ", resource=" + resource + ", sheet=" + sheet + ", force=" + force
				+ ", outputs=" + outputs + ", inputs=" + inputs + "]";
	}

}
