package net.a.g.excel.rest;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;

@ApplicationPath("api")
public class ExcelApplication extends javax.ws.rs.core.Application {

	
	HashSet<Object> singletons = new HashSet<Object>();

	public ExcelApplication() {
		singletons.add(new ExcelResource());
	}

	@Override
	public Set<Class<?>> getClasses() {
		HashSet<Class<?>> set = new HashSet<Class<?>>();
		return set;
	}

	@Override
	public Set<Object> getSingletons() {
		return singletons;
	}


}