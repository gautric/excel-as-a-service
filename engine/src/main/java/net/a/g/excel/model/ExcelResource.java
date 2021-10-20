package net.a.g.excel.model;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import net.a.g.excel.util.ExcelConstants;

import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(value = Include.NON_NULL)
public class ExcelResource extends ExcelModel {
	@JsonProperty("name")
	private String name;

	@JsonProperty("file")
	private String file;

	@JsonIgnore(value = true)
	private byte[] doc;


	public ExcelResource() {
	}

	public ExcelResource(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}

	public byte[] getDoc() {
		return doc;
	}

	public void setDoc(byte[] doc) {
		this.doc = doc;
	}


	public String toString() {
		return "ExcelResource [name=" + name + ", file=" + file + ", doc=" + Arrays.toString(doc).substring(0, 10)
				+ ".... ]";
	}
}
