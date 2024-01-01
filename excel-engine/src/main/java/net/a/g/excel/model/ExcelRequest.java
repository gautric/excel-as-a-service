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

@JsonInclude(value = Include.NON_NULL)
@Schema(name = "ExcelRequest", description = "POJO that represents the request contents.")
public class ExcelRequest {

	@JsonProperty("uuid")
	@Schema(name = "uuid", description = "UUID request")
	private String uuid;

	@JsonProperty("resource")
	@Schema(name = "resource", description = "Resource to execute the request")
	private String resource;

	@JsonProperty("sheet")
	@Schema(name = "sheet", description = "Default sheet request")
	private String sheet;

	@JsonProperty("force")
	@Schema(name = "force", description = "Force compute")
	private boolean force;

	@JsonProperty("outputs")
	@Schema(name = "outputs", description = "List of cell adress")
	private List<String> outputs = new ArrayList<String>();

	@JsonProperty("inputs")
	@Schema(name = "inputs", description = "Map of cell and value")
	private Map<String, String> inputs = new HashMap<String, String>();

	public ExcelRequest() {
		uuid = UUID.randomUUID().toString();
	}

	public ExcelRequest(String uuid) {
		this.uuid = uuid;
	}

	public String getUuid() {
		return this.uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getResource() {
		return this.resource;
	}

	public void setResource(String resource) {
		this.resource = resource;
	}

	public String getSheet() {
		return this.sheet;
	}

	public void setSheet(String sheet) {
		this.sheet = sheet;
	}

	public List<String> getOutputs() {
		return this.outputs;
	}

	public void setOutputs(List<String> outputs) {
		this.outputs = outputs;
	}

	public Map<String, String> getInputs() {
		return this.inputs;
	}

	public void setInputs(Map<String, String> inputs) {
		this.inputs = inputs;
	}

	public boolean isForce() {
		return force;
	}

	public void setForce(boolean force) {
		this.force = force;
	}

	@Override
	public String toString() {
		return "ExcelRequest [uuid=" + uuid + ", resource=" + resource + ", sheet=" + sheet + ", force=" + force
				+ ", outputs=" + outputs + ", inputs=" + inputs + "]";
	}

}
