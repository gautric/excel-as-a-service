package net.a.g.excel.model;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import net.a.g.excel.util.ExcelConstants;

@JsonInclude(value = Include.NON_NULL)
@Schema(name = "ExcelResult", description = "POJO that represents the result contents.")
public class ExcelResult extends ExcelModel {

	@JsonProperty(value = "uuid", namespace = ExcelConstants.SCHEMA_URI)
	@Schema(name = "uuid", description = "UUID request")
	private String uuid;

	@Schema(required = true, description = "Counter of result item")
	@JsonProperty(value = "_count", namespace = ExcelConstants.SCHEMA_URI)
	private int count;

	@JsonProperty(value = "cells", namespace = ExcelConstants.SCHEMA_URI)
	@Schema(oneOf = { ExcelCell[].class })
	private Collection<ExcelCell> cells;

	@JsonProperty(value = "resources", namespace = ExcelConstants.SCHEMA_URI)
	@Schema(oneOf = { ExcelResource[].class })
	private Collection<ExcelResource> resources;

	@JsonProperty(value = "sheets", namespace = ExcelConstants.SCHEMA_URI)
	@Schema(oneOf = { ExcelSheet[].class })
	private Collection<ExcelSheet> sheets;

	@JsonProperty(required = false, value = "error", namespace = ExcelConstants.SCHEMA_URI)
	private ExcelError error;

	public ExcelResult() {
	}

	public ExcelResult(Collection<? extends ExcelModel> results) {
		this.count = results.size();
		if (results.size() > 0 && results.iterator().next() instanceof ExcelSheet) {
			this.sheets = results.stream().map(e -> (ExcelSheet) e).collect(Collectors.toList());

		} else if (results.iterator().next() instanceof ExcelCell) {
			this.cells = results.stream().map(e -> (ExcelCell) e).collect(Collectors.toList());

		} else {
			this.resources = results.stream().map(e -> (ExcelResource) e).collect(Collectors.toList());

		}
	}

	public String getUuid() {
		return this.uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public ExcelError getError() {
		return error;
	}

	public void setError(ExcelError error) {
		this.error = error;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public Collection<ExcelCell> getCells() {
		return this.cells;
	}

	public void setCells(Collection<ExcelCell> cells) {
		this.cells = cells;
	}

	public Collection<ExcelResource> getResources() {
		return this.resources;
	}

	public void setResources(Collection<ExcelResource> resources) {
		this.resources = resources;
	}

	public Collection<ExcelSheet> getSheets() {
		return this.sheets;
	}

	public void setSheets(List<ExcelSheet> sheets) {
		this.sheets = sheets;
	}
}
