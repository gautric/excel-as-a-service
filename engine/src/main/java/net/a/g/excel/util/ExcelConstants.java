package net.a.g.excel.util;

public class ExcelConstants {

	public static final String EXCEL_STATIC_READONLY = "excel.static.readonly";
	public static final String EXCEL_CELL_PATTERN = "([A-Z]{1,5})([0-9]{1,5})";
	public static final String EXCEL_SHEET_CELL_PATTERN = "'?(?<sheetName>[^\\!\\']+)'?!(?<XAxis>[A-Z]{1,5})(?<YAxis>[0-9]{1,5})";

	public static final String EXCEL_SHEET_CELL_PATTERN_STRICT = "(?<first>[^,]+)(,([^,]+))*";

	
	public static final String EXCEL_STATIC_RESOURCE_URI = "excel.static.resouces.uri";
	public static final String EXCEL_STATIC_RESOURCE_NAME = "excel.static.resource.name";
	public static final String EXCEL = "excel";
	public static final String TRUE = "true";
	public static final String DOT = ".";
	public static final String EXCEL_LIST_MAP = "excel.return.list.or.map";
	public static final String EXCEL_RETURN_DEFAULT = "LIST";
	public static final String SCHEMA_URI = "urn://excel-quarkus/v1.0";
	public static final String EXCEL_FORMAT_DATE = "excel.date.format";
	public static final String FORMAT_DATE_ISO =  "yyyy-MM-dd";

}
