package net.a.g.excel.param.cdi;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import net.a.g.excel.param.ExcelParameter;
import net.a.g.excel.util.ExcelConstants;

public class ExcelParameterImpl extends net.a.g.excel.param.ExcelParameterImpl {

	public final static Logger LOG = LoggerFactory.getLogger(ExcelParameter.class);

	public void setReadOnly(
			@ConfigProperty(name = ExcelConstants.EXCEL_STATIC_READONLY, defaultValue = ExcelConstants.TRUE) boolean readOnly) {
		super.setReadOnly(readOnly);
	}

	public void setRetourFormat(
			@ConfigProperty(name = ExcelConstants.EXCEL_LIST_MAP, defaultValue = ExcelConstants.EXCEL_RETURN_DEFAULT) net.a.g.excel.param.ExcelParameterImpl.EXCELRETURN retourFormat) {
		super.setRetourFormat(retourFormat);
	}

	public void setFormatDate(
			@ConfigProperty(name = ExcelConstants.EXCEL_FORMAT_DATE, defaultValue = ExcelConstants.FORMAT_DATE_ISO) String formatDate) {
		super.setFormatDate(formatDate);
	}

	public void setHideFormular(
			@ConfigProperty(name = ExcelConstants.EXCEL_HIDE_FORMULAR, defaultValue = ExcelConstants.EXCEL_HIDE_FORMULAR_DEFAULT) boolean hideFormular) {
		super.setHideFormular(hideFormular);
	}

	@PostConstruct
	public void postConstruc() {
		LOG.info(super.toString());
	}
}
