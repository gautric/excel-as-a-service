package net.a.g.excel.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class POITools {

	public final static Logger LOG = LoggerFactory.getLogger(POITools.class);

	/**
	 * Convert byte[] to Excel POI Workbook
	 * 
	 * @param byteArray
	 * @return
	 */
	public static Workbook convertByteToWorkbook(byte[] byteArray) {
		try {
			return WorkbookFactory.create(new ByteArrayInputStream(byteArray));
		} catch (EncryptedDocumentException ex) {
			LOG.error("Workbook  is not a XSSF file", ex);
		} catch (IOException ex) {
			LOG.error("Workbook  is not a XSSF file", ex);
		}
		return null;
	}
}
