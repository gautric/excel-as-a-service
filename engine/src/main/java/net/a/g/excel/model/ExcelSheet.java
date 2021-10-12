package net.a.g.excel.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(value = Include.NON_NULL)
public class ExcelSheet {
	@JsonProperty("name")
	public String name;

	@JsonProperty("_ref")
	public String ref;

	public ExcelSheet(String name, String ref) {
		this.name = name;
		this.ref = ref;
	}
}
