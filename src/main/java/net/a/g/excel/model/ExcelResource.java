package net.a.g.excel.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(value = Include.NON_NULL)
public class ExcelResource {
	@JsonProperty("name")
	public String name;

	@JsonProperty("file")
	public String file;

	@JsonProperty("_ref")
	public String ref;

	public ExcelResource(String name, String ref) {
		this.name = name;
		this.ref = ref;
	}
}
