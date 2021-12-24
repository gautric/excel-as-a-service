package net.a.g.excel.util;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@Named
public class ExcelConfiguration {

	public final static Logger LOG = LoggerFactory.getLogger(ExcelConfiguration.class);

	enum EXCELRETURN {
		MAP, LIST
	}

	@ConfigProperty(name = ExcelConstants.EXCEL_STATIC_RESOURCE_URI, defaultValue = ExcelConstants.DOT)
	String resouceUri;

	@ConfigProperty(name = ExcelConstants.EXCEL_STATIC_READONLY, defaultValue = ExcelConstants.TRUE)
	boolean readOnly;

	@ConfigProperty(name = ExcelConstants.EXCEL_LIST_MAP, defaultValue = ExcelConstants.EXCEL_RETURN_DEFAULT)
	EXCELRETURN retourFormat = EXCELRETURN.MAP;

	@ConfigProperty(name = ExcelConstants.EXCEL_FORMAT_DATE, defaultValue = ExcelConstants.FORMAT_DATE_ISO)
	String formatDate = ExcelConstants.FORMAT_DATE_ISO;

	@ConfigProperty(name = ExcelConstants.EXCEL_HIDE_FORMULAR, defaultValue = ExcelConstants.EXCEL_HIDE_FORMULAR_DEFAULT)
	boolean hideFormular = false;

	public EXCELRETURN getFormatRetour() {
		return retourFormat;
	}

	public String getFormatDate() {
		return formatDate;
	}

	public String getResouceUri() {
		return resouceUri;
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
		return "ExcelConfiguration [resouceUri=" + this.resouceUri + ", readOnly=" + this.readOnly + ", retourFormat="
				+ this.retourFormat + ", formatDate=" + this.formatDate + ", hideFormular=" + this.hideFormular + "]";
	}

	@PostConstruct
	public void postConstruc() {
		LOG.info(this.toString());
	}

}
