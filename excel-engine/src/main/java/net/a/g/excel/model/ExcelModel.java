package net.a.g.excel.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import net.a.g.excel.util.ExcelConstants;

public abstract class ExcelModel {

	@JsonProperty(value = "_links", namespace = ExcelConstants.SCHEMA_URI)
	private List<ExcelLink> links = new ArrayList<ExcelLink>();

	public ExcelModel() {
		super();
	}

	public List<ExcelLink> getLinks() {
		return this.links;
	}

	public void setLinks(List<ExcelLink> links) {
		this.links = links;
	}

}