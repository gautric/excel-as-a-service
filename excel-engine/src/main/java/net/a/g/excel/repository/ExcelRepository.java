package net.a.g.excel.repository;

import java.util.Collection;

import org.apache.poi.ss.usermodel.Workbook;

import net.a.g.excel.model.ExcelResource;

/**
 * Repository interface for managing Excel resources.
 * This interface provides methods to store, retrieve, and manage Excel workbooks and their associated resources.
 * It implements the Repository pattern for Excel document management.
 */
public interface ExcelRepository {

    /**
     * Adds a new Excel resource to the repository.
     *
     * @param resource the Excel resource to add
     * @return true if the resource was successfully added, false otherwise
     */
    boolean add(ExcelResource resource);

    /**
     * Returns the total number of Excel resources in the repository.
     *
     * @return the count of resources
     */
    int count();

    /**
     * @deprecated Use {@link #listOfResources()} instead
     * Returns a collection of all Excel resources in the repository.
     *
     * @return collection of Excel resources
     */
    @Deprecated
    Collection<ExcelResource> listOfResource();

    /**
     * Returns a collection of all Excel resources in the repository.
     *
     * @return collection of Excel resources
     */
    Collection<ExcelResource> listOfResources();

    /**
     * Removes all Excel resources from the repository.
     */
    void purge();

    /**
     * Checks if a resource with the specified name exists in the repository.
     *
     * @param name the name of the resource to check
     * @return true if the resource exists, false otherwise
     */
    boolean contains(String name);

    /**
     * Retrieves an Excel resource by its name.
     *
     * @param name the name of the resource to retrieve
     * @return the Excel resource, or null if not found
     */
    ExcelResource get(String name);

    /**
     * Retrieves a POI Workbook by its resource name.
     *
     * @param name the name of the workbook to retrieve
     * @return the POI Workbook object, or null if not found
     * @see org.apache.poi.ss.usermodel.Workbook
     */
    Workbook retrieveWorkbook(String name);

    /**
     * Opens a fresh, private copy of the workbook. The caller owns it (may
     * mutate it) and must close it. Use it for any write or formula
     * evaluation, so concurrent requests never share state and the cached
     * template keeps its formulas.
     *
     * @param name the resource name
     * @return a new workbook parsed from the stored document
     */
    default Workbook openWorkbook(String name) {
        Workbook wb = net.a.g.excel.util.POITools.convertByteToWorkbook(get(name).getDoc());
        if (wb == null) {
            throw new net.a.g.excel.exception.ExcelEngineException("Failed to load workbook: " + name);
        }
        return wb;
    }

    /**
     * Removes a resource from the repository.
     *
     * @param name the resource name
     * @return true if a resource was removed
     */
    default boolean remove(String name) {
        throw new UnsupportedOperationException("remove not supported");
    }
}
