package net.a.g.excel.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExcelUtils {

	/**
	 * Return X position of letter 'D' = 4
	 * 
	 * 
	 * @param a
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
	 * @return
	 */
	public static int[] getPosition(String address) {
		int[] ret = { 0, 0 };
		Pattern p = Pattern.compile(ExcelConstants.EXCEL_CELL_PATTERN);
		Matcher m = p.matcher(address);
		if (m.matches()) {
			ret[0] = position(m.group(1));
			ret[1] = Integer.parseInt(m.group(2)) - 1;
		}
		return ret;
	}

	
	public static boolean checkAdress(String address) {
		Pattern p = Pattern.compile(ExcelConstants.EXCEL_CELL_PATTERN);
		Matcher m = p.matcher(address);
		return m.matches();
	}
	
}
