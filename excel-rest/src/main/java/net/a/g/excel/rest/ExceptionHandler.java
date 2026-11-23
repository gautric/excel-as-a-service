package net.a.g.excel.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import net.a.g.excel.exception.ExcelInvalidInputException;
import net.a.g.excel.exception.ExcelResourceNotFoundException;

/**
 * Maps exceptions to HTTP status codes, with the same {@code {code, message}}
 * body as the explicit 404 answers:
 * <ul>
 * <li>{@link ExcelResourceNotFoundException} -> 404</li>
 * <li>{@link ExcelInvalidInputException}, {@link IllegalArgumentException} -> 400</li>
 * <li>{@link WebApplicationException} 4xx -> its own status</li>
 * <li>anything else -> 500 with a generic message: the stack trace is logged,
 * never sent to the client</li>
 * </ul>
 */
@Provider
public class ExceptionHandler implements ExceptionMapper<Exception> {

	public final static Logger LOG = LoggerFactory.getLogger(ExceptionHandler.class);

	@Override
	public Response toResponse(Exception exception) {

		if (exception instanceof ExcelResourceNotFoundException) {
			return ExcelRestTool.error(Status.NOT_FOUND, exception.getMessage());
		}
		if (exception instanceof ExcelInvalidInputException || exception instanceof IllegalArgumentException) {
			return ExcelRestTool.error(Status.BAD_REQUEST, exception.getMessage());
		}
		if (exception instanceof WebApplicationException wae && wae.getResponse().getStatus() < 500) {
			int code = wae.getResponse().getStatus();
			Status status = Status.fromStatusCode(code);
			return ExcelRestTool.error(status == null ? Status.BAD_REQUEST : status, exception.getMessage());
		}

		LOG.error("REST API unexpected error", exception);
		return ExcelRestTool.error(Status.INTERNAL_SERVER_ERROR, "Internal server error");
	}

}
