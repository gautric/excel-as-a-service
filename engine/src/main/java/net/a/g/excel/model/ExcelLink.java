package net.a.g.excel.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(value = Include.NON_NULL)
@Schema(name = "ExcelLink", description = "POJO that represents a ExcelLink.")
public class ExcelLink {

	@JsonProperty("rel")
	@Schema(name = "rel", description = "Rel")
	private String rel;

	@JsonProperty("href")
	@Schema(name = "href", description = "Link")
	private String href;

	@JsonProperty("type")
	@Schema(name = "type", description = "Type")
	private String type;

	@JsonProperty("method")
	@Schema(name = "method", description = "Method")
	private String method;

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getRel() {
		return rel;
	}

	public void setRel(String rel) {
		this.rel = rel;
	}

	public String getHref() {
		return href;
	}

	public void setHref(String href) {
		this.href = href;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

}
