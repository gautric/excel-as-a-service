package net.a.g.excel.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import net.a.g.excel.util.ExcelConstants;

/**
 * Abstract base class for Excel-related model classes.
 * Implements HATEOAS (Hypermedia as the Engine of Application State) support
 * by providing a collection of links that describe available actions and
 * related resources. All Excel model classes should extend this class to
 * inherit the hypermedia capabilities.
 */
public abstract class ExcelModel {

    /** Collection of HATEOAS links associated with the model */
    @JsonProperty(value = "_links", namespace = ExcelConstants.SCHEMA_URI)
    private List<ExcelLink> links = new ArrayList<ExcelLink>();

    /**
     * Default constructor.
     * Initializes an empty list of links.
     */
    public ExcelModel() {
		super();
	}

    /**
     * Gets the list of HATEOAS links associated with this model.
     * @return list of ExcelLink objects
     */
    public List<ExcelLink> getLinks() {
		return this.links;
	}

    /**
     * Sets the list of HATEOAS links for this model.
     * @param links the list of ExcelLink objects to set
     */
    public void setLinks(List<ExcelLink> links) {
		this.links = links;
	}

}
