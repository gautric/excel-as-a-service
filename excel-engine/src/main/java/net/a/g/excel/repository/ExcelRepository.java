package net.a.g.excel.repository;

import java.util.Collection;

import org.apache.poi.ss.usermodel.Workbook;

import net.a.g.excel.model.ExcelResource;

public interface ExcelRepository {

	boolean add(ExcelResource resource);

	int count();

	Collection<ExcelResource> listOfResource();

	Collection<ExcelResource> listOfResources();

	void purge();

	boolean contains(String name);

	ExcelResource get(String name);

	Workbook retrieveWorkbook(String name);
}