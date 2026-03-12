package net.a.g.excel.repository;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.a.g.excel.exception.ExcelEngineException;
import net.a.g.excel.exception.ExcelResourceNotFoundException;
import net.a.g.excel.model.ExcelResource;
import net.a.g.excel.util.POITools;
import net.a.g.excel.validation.InputValidator;

public class ExcelRepositoryImpl implements ExcelRepository, AutoCloseable {

	public static final Logger LOG = LoggerFactory.getLogger(ExcelRepositoryImpl.class);

	// Use ConcurrentHashMap for thread-safety
	private Map<String, ExcelResource> listOfResources = new ConcurrentHashMap<>();
	
	// Cache for opened workbooks to manage resources
	private Map<String, Workbook> workbookCache = new ConcurrentHashMap<>();
	
	private volatile boolean closed = false;

	public boolean add(ExcelResource resource) {
		checkNotClosed();
		
		if (resource == null) {
			throw new IllegalArgumentException("Resource cannot be null");
		}
		
		// Validate resource name
		InputValidator.validateResourceName(resource.getName());
		
		// Validate the workbook can be loaded
		Workbook testWorkbook = null;
		try {
			testWorkbook = POITools.convertByteToWorkbook(resource.getDoc());
			if (testWorkbook == null) {
				LOG.error("Workbook {} is not readable", resource.getName());
				return false;
			}
		} catch (Exception e) {
			LOG.error("Failed to load workbook {}: {}", resource.getName(), e.getMessage());
			throw new ExcelEngineException("Failed to add resource: " + resource.getName(), e);
		} finally {
			// Close test workbook to prevent resource leak
			if (testWorkbook != null) {
				try {
					testWorkbook.close();
				} catch (IOException e) {
					LOG.warn("Failed to close test workbook: {}", e.getMessage());
				}
			}
		}
		
		// Close existing cached workbook if replacing
		if (listOfResources.containsKey(resource.getName())) {
			closeWorkbook(resource.getName());
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
		checkNotClosed();
		
		// Close all cached workbooks before clearing
		closeAllWorkbooks();
		
		listOfResources.clear();
		LOG.info("Repository purged");
	}

	public boolean contains(String name) {
		return listOfResources.containsKey(name);
	}

	public ExcelResource get(String name) {
		checkNotClosed();
		
		if (name == null || name.trim().isEmpty()) {
			throw new IllegalArgumentException("Resource name cannot be null or empty");
		}
		
		ExcelResource resource = listOfResources.get(name);
		if (resource == null) {
			throw new ExcelResourceNotFoundException(name);
		}
		
		return resource;
	}

	public Workbook retrieveWorkbook(String name) {
		checkNotClosed();
		
		if (name == null || name.trim().isEmpty()) {
			throw new IllegalArgumentException("Resource name cannot be null or empty");
		}
		
		if (!contains(name)) {
			throw new ExcelResourceNotFoundException(name);
		}
		
		// Return cached workbook if available
		Workbook cachedWorkbook = workbookCache.get(name);
		if (cachedWorkbook != null) {
			return cachedWorkbook;
		}
		
		// Load and cache the workbook
		try {
			byte[] byteArray = get(name).getDoc();
			Workbook workbook = POITools.convertByteToWorkbook(byteArray);
			
			if (workbook == null) {
				throw new ExcelEngineException("Failed to load workbook: " + name);
			}
			
			workbookCache.put(name, workbook);
			LOG.debug("Workbook {} loaded and cached", name);
			
			return workbook;
		} catch (Exception e) {
			LOG.error("Failed to retrieve workbook {}: {}", name, e.getMessage());
			throw new ExcelEngineException("Failed to retrieve workbook: " + name, e);
		}
	}
	
	/**
	 * Closes a specific workbook and removes it from cache.
	 *
	 * @param name the name of the workbook to close
	 */
	private void closeWorkbook(String name) {
		Workbook workbook = workbookCache.remove(name);
		if (workbook != null) {
			try {
				workbook.close();
				LOG.debug("Workbook {} closed", name);
			} catch (IOException e) {
				LOG.warn("Failed to close workbook {}: {}", name, e.getMessage());
			}
		}
	}
	
	/**
	 * Closes all cached workbooks.
	 */
	private void closeAllWorkbooks() {
		for (String name : workbookCache.keySet()) {
			closeWorkbook(name);
		}
		workbookCache.clear();
	}
	
	/**
	 * Checks if the repository has been closed.
	 *
	 * @throws IllegalStateException if the repository is closed
	 */
	private void checkNotClosed() {
		if (closed) {
			throw new IllegalStateException("Repository has been closed");
		}
	}
	
	/**
	 * Closes the repository and releases all resources.
	 * This method closes all cached workbooks and prevents further operations.
	 */
	@Override
	public void close() {
		if (!closed) {
			closed = true;
			closeAllWorkbooks();
			LOG.info("Repository closed");
		}
	}

}
