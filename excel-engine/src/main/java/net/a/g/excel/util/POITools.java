package net.a.g.excel.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for Apache POI (Poor Obfuscation Implementation) operations.
 * This class provides helper methods for working with Excel workbooks using Apache POI library.
 * 
 * @see org.apache.poi.ss.usermodel.Workbook
 * @see org.apache.poi.ss.usermodel.WorkbookFactory
 */
public class POITools {

	public final static Logger LOG = LoggerFactory.getLogger(POITools.class);

	/**
	 * Converts a byte array containing Excel file data into a POI Workbook object.
	 * This method supports both .xls and .xlsx file formats.
	 * 
	 * @param byteArray the byte array containing Excel file data
	 * @return a Workbook object representing the Excel file, or null if conversion fails
	 * @throws EncryptedDocumentException if the document is password protected
	 * @throws IOException if there is an error reading the byte array
	 * @see org.apache.poi.ss.usermodel.WorkbookFactory#create(java.io.InputStream)
	 */
	public static Workbook convertByteToWorkbook(byte[] byteArray) {
		try {
			return WorkbookFactory.create(new ByteArrayInputStream(byteArray));
		} catch (EncryptedDocumentException ex) {
			LOG.error("Workbook is encrypted or password protected", ex);
		} catch (IOException ex) {
			LOG.error("Error reading workbook data", ex);
		}
		return null;
	}
}
