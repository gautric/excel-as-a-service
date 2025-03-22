package net.a.g.excel.load;

import java.io.InputStream;

/**
 * Interface for loading Excel resources into the system.
 * Provides functionality to inject Excel workbooks from input streams
 * into the Excel processing system.
 */
public interface ExcelLoader {

    /**
     * Injects an Excel resource into the system from an input stream.
     * 
     * @param resourceName the unique name to identify the resource in the system
     * @param resourceFileName the original file name of the resource (can be null)
     * @param resourceStream the input stream containing the Excel workbook data
     * @return true if the resource was successfully injected, false otherwise
     */
    boolean injectResource(String resourceName, String resourceFileName, InputStream resourceStream);

}
