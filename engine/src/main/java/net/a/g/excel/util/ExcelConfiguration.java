package net.a.g.excel.util;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.config.inject.ConfigProducer;

@ApplicationScoped
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
	EXCELRETURN list;
	
	@Inject 
	ConfigProducer config;

	
	public String getResouceUri() {
		return resouceUri;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public boolean returnList() {
		return list ==  EXCELRETURN.LIST;
	}
	
	public boolean returnMap() {
		return list ==  EXCELRETURN.MAP;
	}
	
	@Override
	public String toString() {
		return "ExcelConfiguration [resouceUri=" + resouceUri + ", readOnly=" + isReadOnly() + "]";
	}

	@PostConstruct
	public void postConstruc() {
		LOG.info(this.toString());
		LOG.info("{}", config	 );
		
	
		
	}

}
