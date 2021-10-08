package net.a.g.excel.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(value = Include.NON_NULL)
public class ExcelResult {
	@JsonProperty("_count")
	public int count;

	@JsonProperty("_next")
	public String next;

	@JsonProperty("_previous")
	public String previous;
	
	@JsonProperty("_self")
	public String self;

	@JsonProperty("results")
	public Object results;

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
