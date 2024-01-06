package net.a.g.excel.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExcelUtils {

	public final static Logger LOG = LoggerFactory.getLogger(ExcelUtils.class);

	/**
	 * Return X position of letter 'D' = 4
	 * 
	 * @param a letter
	 * @return
	 */
	public static int position(String a) {
		final int A = 65;
		int result = 0;
		for (int i = 0; i < a.length(); i++) {
			int p = ((int) a.charAt(i) - A) + 26 * (a.length() - i - 1);
			result = result + p;
		}
		return result;
	}

	/**
	 * Return X,Y cell position from Excel position
	 * 
	 * D1 => {0,4}
	 * 
	 * @param address
	 * @return { -1, -1 } if address doesn't match
	 */
	public static int[] getPosition(String address) {
		int[] ret = { -1, -1 };
		Pattern p = Pattern.compile(ExcelConstants.EXCEL_CELL_PATTERN);
		Matcher m = p.matcher(address);
		if (m.matches()) {
			ret[0] = position(m.group(1));
			ret[1] = Integer.parseInt(m.group(2)) - 1;
		}
		return ret;
	}

	/**
	 * Return true if address match with Excel cell format address
	 * 
	 * @param address
	 * @return 
	 */
	public static boolean checkAdress(String address) {
		Pattern p = Pattern.compile(ExcelConstants.EXCEL_CELL_PATTERN);
		Matcher m = p.matcher(address);
		return m.matches();
	}

	/**
	 * Check if address matches with Sheet!XXNN pattern
	 * 
	 * @param address
	 * @return true/false
	 */
	public static boolean checkFullAdress(String address) {
		Pattern p = Pattern.compile(ExcelConstants.EXCEL_SHEET_CELL_PATTERN_V2);
		Matcher m = p.matcher(address);
		boolean ret = m.matches();
		if (ret && LOG.isTraceEnabled()) {
			LOG.trace("sheet [{}] X [{}] Y [{}] ", m.group("sheetName"), m.group("XAxis"), m.group("YAxis"));
		}
		return ret;
	}

	/**
	 * Check if all address match with Sheet!XXNN pattern
	 * 
	 * @param address
	 * @return true/false
	 */
	public static boolean checkFullAdressStrict(String address) {
		return Arrays.asList(address.split(",")).stream().allMatch(ExcelUtils::checkFullAdress);
	}

	/**
	 * Filter all address match with Sheet!XXNN pattern
	 * 
	 * @param address
	 * @return true/false
	 */
	public static List<String> filterValidAdress(String address) {
		return Arrays.asList(address.split(",")).stream().filter(ExcelUtils::checkFullAdress)
				.collect(Collectors.toList());
	}


}
