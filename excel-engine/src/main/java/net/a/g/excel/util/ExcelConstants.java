package net.a.g.excel.util;

/**
 * Constants used throughout the Excel-as-a-service application.
 * 
 * This class provides centralized access to:
 * - Regular expression patterns for parsing Excel cell references
 * - Configuration property names
 * - Default values for various settings
 * - Other utility constants
 */
public class ExcelConstants {

	/** Configuration property to set Excel resources as read-only */
	public static final String EXCEL_STATIC_READONLY = "excel.static.readonly";
	
	/** Regular expression pattern to match Excel cell references (e.g., A1, BC23) */
	public static final String EXCEL_CELL_PATTERN = "([A-Z]{1,5})([0-9]{1,5})";
	
	/** Regular expression pattern to match Excel sheet and cell references (e.g., Sheet1!A1) */
	public static final String EXCEL_SHEET_CELL_PATTERN = "'?(?<sheetName>[^\\!\\']+)'?!(?<XAxis>[A-Z]{1,5})(?<YAxis>[0-9]{1,5})";
	
	/** Enhanced regular expression pattern for Excel sheet and cell references with better handling of sheet names */
	public static final String EXCEL_SHEET_CELL_PATTERN_V2 = "(?<sheetName>('([^\\!\\']+)')|(([^\\!\\'\\s]+)))!(?<XAxis>[A-Z]{1,5})(?<YAxis>[0-9]{1,5})";

	/** Regular expression pattern for strict validation of comma-separated values */
	public static final String EXCEL_SHEET_CELL_PATTERN_STRICT = "(?<first>[^,]+)(,([^,]+))*";

	/** Configuration property for the URI of static Excel resources */
	public static final String EXCEL_STATIC_RESOURCE_URI = "excel.static.resouces.uri";
	
	/** Configuration property for the name of static Excel resources */
	public static final String EXCEL_STATIC_RESOURCE_NAME = "excel.static.resource.name";
	
	/** Common prefix for Excel-related properties */
	public static final String EXCEL = "excel";
	
	/** String constant for boolean true value */
	public static final String TRUE = "true";
	
	/** String constant for dot character */
	public static final String DOT = ".";
	
	/** Configuration property to determine if results should be returned as a list or map */
	public static final String EXCEL_LIST_MAP = "excel.return.list.or.map";
	
	/** Default return type for Excel operations */
	public static final String EXCEL_RETURN_DEFAULT = "MAP";
	
	/** URI for the Excel-as-a-service schema */
	public static final String SCHEMA_URI = "urn://excel-as-a-service/v1.0";
	
	/** Configuration property for date formatting in Excel */
	public static final String EXCEL_FORMAT_DATE = "excel.date.format";
	
	/** Default ISO date format pattern */
	public static final String FORMAT_DATE_ISO =  "yyyy-MM-dd";
	
	/** Configuration property to hide Excel formulas */
	public static final String EXCEL_HIDE_FORMULAR = "excel.hide.formular";
	
	/** Default value for hiding Excel formulas */
	public static final String EXCEL_HIDE_FORMULAR_DEFAULT = "false";
}
