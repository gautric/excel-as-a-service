package net.a.g.excel.param.cdi;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import net.a.g.excel.param.ExcelParameter;
import net.a.g.excel.util.ExcelConstants;

/**
 * Parameters read from MicroProfile Config at startup. {@link ConfigProvider}
 * is used (like the loader) so it works in Quarkus and in plain Weld SE.
 */
@ApplicationScoped
@Named
public class ExcelParameterImpl extends net.a.g.excel.param.ExcelParameterImpl {

	public final static Logger LOG = LoggerFactory.getLogger(ExcelParameter.class);

	@PostConstruct
	public void postConstruc() {
		Config config = ConfigProvider.getConfig();
		setReadOnly(Boolean.parseBoolean(value(config, ExcelConstants.EXCEL_STATIC_READONLY, ExcelConstants.TRUE)));
		setRetourFormat(EXCELRETURN
				.valueOf(value(config, ExcelConstants.EXCEL_LIST_MAP, ExcelConstants.EXCEL_RETURN_DEFAULT).trim()));
		setFormatDate(value(config, ExcelConstants.EXCEL_FORMAT_DATE, ExcelConstants.FORMAT_DATE_ISO));
		setHideFormular(Boolean.parseBoolean(
				value(config, ExcelConstants.EXCEL_HIDE_FORMULAR, ExcelConstants.EXCEL_HIDE_FORMULAR_DEFAULT)));
		LOG.info(super.toString());
	}

	private static String value(Config config, String name, String defaultValue) {
		return config.getOptionalValue(name, String.class).orElse(defaultValue);
	}
}
