package net.a.g.excel.param;

public interface ExcelParameter {

	public enum EXCELRETURN {
		MAP, LIST
	}

	EXCELRETURN getFormatRetour();

	String getFormatDate();

	boolean isReadOnly();

	boolean returnList();

	boolean returnMap();

	EXCELRETURN getRetourFormat();

	boolean isHideFormular();

	String toString();

	void setReadOnly(boolean readOnly);

	void setRetourFormat(EXCELRETURN retourFormat);

	void setFormatDate(String formatDate);

	void setHideFormular(boolean hideFormular);

}