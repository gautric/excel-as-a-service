package net.a.g.excel.util;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ExcelConfiguration {
	
	public final static Logger LOG = LoggerFactory.getLogger(ExcelConfiguration.class);


	@ConfigProperty(name = ExcelConstants.EXCEL_STATIC_RESOURCE_URI, defaultValue = "../Classeur1.xlsx")
	String resouceUri;
	@ConfigProperty(name = ExcelConstants.EXCEL_STATIC_READONLY, defaultValue = ExcelConstants.TRUE)
	boolean readOnly;

	public String getResouceUri() {
		return resouceUri;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	
	
	@Override
	public String toString() {
		return "ExcelConfiguration [resouceUri=" + resouceUri + ", readOnly=" + isReadOnly() + "]";
	}

	@PostConstruct
	public void postConstruc() {
		LOG.info(this.toString());
	}
	
}
