package net.a.g.excel.rest;

import com.fasterxml.jackson.core.JsonProcessingException;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** A request body that is not valid JSON answers 400 with the usual {@code {code, message}} body. */
@Provider
public class JsonErrorHandler implements ExceptionMapper<JsonProcessingException> {

	@Override
	public Response toResponse(JsonProcessingException exception) {
		return ExcelRestTool.error(Status.BAD_REQUEST, "Invalid JSON body: " + exception.getOriginalMessage());
	}
}
