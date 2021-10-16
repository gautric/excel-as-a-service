package net.a.g.excel.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import net.a.g.excel.util.ExcelConstants;

@JsonInclude(value = Include.NON_NULL)
@Schema(name = "ExcelResult", description = "POJO that represents the result contents.")
public class ExcelResult {
	@Schema(required = true, description = "Counter of result item")
	@JsonProperty(value = "_count", namespace = ExcelConstants.SCHEMA_URI)
	public int count;

	@JsonProperty(value = "_next", namespace = ExcelConstants.SCHEMA_URI)
	public String next;

	@JsonProperty(value = "_previous", namespace = ExcelConstants.SCHEMA_URI)
	public String previous;

	@JsonProperty(value = "_self", namespace = ExcelConstants.SCHEMA_URI)
	public String self;

	@JsonProperty(value = "results", namespace = ExcelConstants.SCHEMA_URI)
	@Schema(oneOf = { ExcelCell[].class, ExcelResource[].class, ExcelSheet[].class })
	public Object results;

	@JsonProperty(required = false, value = "error", namespace = ExcelConstants.SCHEMA_URI)
	public ExcelError error;
	
	@JsonProperty(value = "links", namespace = ExcelConstants.SCHEMA_URI)
	public ExcelLink[] links;
	
	public ExcelError getError() {
		return error;
	}

	public void setError(ExcelError error) {
		this.error = error;
	}

	public String getSelf() {
		return self;
	}

	public void setSelf(String self) {
		this.self = self;
	}

	public ExcelResult() {
	}

	public ExcelResult(int count, Object results) {
		this.count = count;
		this.results = results;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public String getNext() {
		return next;
	}

	public void setNext(String next) {
		this.next = next;
	}

	public String getPrevious() {
		return previous;
	}

	public void setPrevious(String previous) {
		this.previous = previous;
	}

	public Object getResults() {
		return results;
	}

	public void setResults(Object results) {
		this.results = results;
	}
}
