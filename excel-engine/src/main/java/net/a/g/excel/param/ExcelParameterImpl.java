package net.a.g.excel.param;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.a.g.excel.util.ExcelConstants;

public class ExcelParameterImpl implements ExcelParameter {

	public final static Logger LOG = LoggerFactory.getLogger(ExcelParameterImpl.class);

	boolean readOnly;

	EXCELRETURN retourFormat = EXCELRETURN.MAP;

	String formatDate = ExcelConstants.FORMAT_DATE_ISO;

	boolean hideFormular = false;

	public EXCELRETURN getFormatRetour() {
		return retourFormat;
	}

	public String getFormatDate() {
		return formatDate;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public boolean returnList() {
		return retourFormat == EXCELRETURN.LIST;
	}

	public boolean returnMap() {
		return retourFormat == EXCELRETURN.MAP;
	}

	public EXCELRETURN getRetourFormat() {
		return this.retourFormat;
	}

	public boolean isHideFormular() {
		return this.hideFormular;
	}

	@Override
	public String toString() {
		return "ExcelParameterImpl [readOnly=" + this.readOnly + ", retourFormat=" + this.retourFormat + ", formatDate="
				+ this.formatDate + ", hideFormular=" + this.hideFormular + "]";
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public void setRetourFormat(EXCELRETURN retourFormat) {
		this.retourFormat = retourFormat;
	}

	public void setFormatDate(String formatDate) {
		this.formatDate = formatDate;
	}

	public void setHideFormular(boolean hideFormular) {
		this.hideFormular = hideFormular;
	}
}
