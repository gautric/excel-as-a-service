package net.a.g.excel.repository;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import net.a.g.excel.engine.ExcelEngineImpl;
import net.a.g.excel.model.ExcelResource;
import net.a.g.excel.util.ExcelUtils;

public class ExcelRepositoryImpl implements ExcelRepository {

	public static final Logger LOG = LoggerFactory.getLogger(ExcelEngineImpl.class);

	private Map<String, ExcelResource> listOfResources = new HashMap<String, ExcelResource>();

	public boolean add(ExcelResource resource) {

		assert (resource != null);

		if (ExcelUtils.convertByteToWorkbook(resource.getDoc()) == null) {
			LOG.error("Workbook {} is not readable", resource.getName());
			return false;
		}
		listOfResources.put(resource.getName(), resource);

		LOG.info("Add {} OK", resource);

		return true;
	}

	public int count() {
		return listOfResources.keySet().size();
	}

	public Collection<ExcelResource> listOfResource() {
		return listOfResources.values();
	}

	public Collection<ExcelResource> listOfResources() {
		return listOfResources.values();
	}

	public void purge() {
		listOfResources.clear();
	}

	public boolean contains(String name) {
		return listOfResources.containsKey(name);
	}

	public ExcelResource get(String name) {
		return listOfResources.get(name);
	}

	public Workbook retrieveWorkbook(String name) {
		Workbook workbook = null;
		if (contains(name)) {
			byte[] byteArray = get(name).getDoc();
			workbook = ExcelUtils.convertByteToWorkbook(byteArray);
		}
		return workbook;
	}

}
