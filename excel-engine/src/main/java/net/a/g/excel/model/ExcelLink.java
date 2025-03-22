package net.a.g.excel.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a HATEOAS-style link for Excel resources.
 * This class implements the HATEOAS (Hypermedia as the Engine of Application State) pattern,
 * providing navigational information between Excel resources. It supports JSON serialization
 * with null value exclusion and OpenAPI documentation.
 */
@JsonInclude(value = Include.NON_NULL)
@Schema(name = "ExcelLink", description = "POJO that represents a HATEOAS-style link for Excel resources.")
public class ExcelLink {

    /** The relationship type of the link (e.g., "self", "next", "previous") */
    @JsonProperty("rel")
    @Schema(name = "rel", description = "The relationship type of the link")
    private String rel;

    /** The URL or URI of the linked resource */
    @JsonProperty("href")
    @Schema(name = "href", description = "The URL or URI of the linked resource")
    private String href;

    /** The media type of the linked resource */
    @JsonProperty("type")
    @Schema(name = "type", description = "The media type of the linked resource")
    private String type;

    /** The HTTP method to use when accessing the linked resource */
    @JsonProperty("method")
    @Schema(name = "method", description = "The HTTP method to use when accessing the resource")
    private String method;

    /**
     * Gets the HTTP method.
     * @return the HTTP method (e.g., GET, POST, PUT)
     */
    public String getMethod() {
		return method;
	}

    /**
     * Sets the HTTP method.
     * @param method the HTTP method to set
     */
    public void setMethod(String method) {
		this.method = method;
	}

    /**
     * Gets the relationship type.
     * @return the relationship type of the link
     */
    public String getRel() {
		return rel;
	}

    /**
     * Sets the relationship type.
     * @param rel the relationship type to set
     */
    public void setRel(String rel) {
		this.rel = rel;
	}

    /**
     * Gets the URL or URI of the linked resource.
     * @return the resource URL/URI
     */
    public String getHref() {
		return href;
	}

    /**
     * Sets the URL or URI of the linked resource.
     * @param href the resource URL/URI to set
     */
    public void setHref(String href) {
		this.href = href;
	}

    /**
     * Gets the media type of the linked resource.
     * @return the media type
     */
    public String getType() {
		return type;
	}

    /**
     * Sets the media type of the linked resource.
     * @param type the media type to set
     */
    public void setType(String type) {
		this.type = type;
	}

}
