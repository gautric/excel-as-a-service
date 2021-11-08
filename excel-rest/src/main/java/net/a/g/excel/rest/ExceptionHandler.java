package net.a.g.excel.rest;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.a.g.excel.model.ExcelError;
import net.a.g.excel.model.ExcelResult;

@Provider
public class ExceptionHandler implements ExceptionMapper<Exception> {
	
	public final static Logger LOG = LoggerFactory.getLogger(ExceptionHandler.class);

	public Response toResponse(Exception exception) {
		
		LOG.error("REST API Catch Exception", exception);

		
		ExcelResult ret = new ExcelResult();
		ExcelError err = new ExcelError();

		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		exception.printStackTrace(pw);

		err.setCode(exception.getClass().getCanonicalName());
		err.setError(sw.toString());
		ret.setError(err);
		
		return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ret).build();
	}

}