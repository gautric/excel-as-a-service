package net.a.g.excel.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(value = Include.NON_NULL)
public class ExcelCell {
	@JsonProperty("address")
	public String address;

	@JsonProperty("formula")
	public String formula;

	@JsonProperty("value")
	public Object value;
	
	@JsonProperty("_metadata")
	public String metadata;

	@JsonProperty("_ref")
	public String ref;

	public ExcelCell() {
	};

	public ExcelCell(String adress, String formula, Object value, String ref) {
		this.address = adress;
		this.formula = formula;
		this.value = value;
		this.ref = ref;
	};

}
