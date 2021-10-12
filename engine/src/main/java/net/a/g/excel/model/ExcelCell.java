package net.a.g.excel.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(value = Include.NON_NULL)
public class ExcelCell {
	@JsonProperty("address")
	private String address;

	@JsonProperty("value")
	private Object value;
	
	@JsonProperty("_metadata")
	private String metadata;
	
	@JsonProperty("type")
	private String type;

	@JsonProperty("_ref")
	private String ref;

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}


	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public String getMetadata() {
		return metadata;
	}

	public void setMetadata(String metadata) {
		this.metadata = metadata;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getRef() {
		return ref;
	}

	public void setRef(String ref) {
		this.ref = ref;
	}

	public ExcelCell() {
	};

	public ExcelCell(String adress, String formula, Object value, String ref, String metadata) {
		this.address = adress;
		this.value = value;
		this.ref = ref;
	};

}
