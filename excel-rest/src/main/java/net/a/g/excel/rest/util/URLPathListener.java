package net.a.g.excel.rest.util;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.http.HttpServerRequest;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;

/**
 * A JAX-RS response filter that logs the absolute path of incoming HTTP requests.
 * 
 * <p>This class is annotated with {@code @Provider}, which allows it to be automatically
 * discovered and registered by the JAX-RS runtime as a container response filter.
 * It implements the {@link ContainerResponseFilter} interface to intercept and
 * potentially modify HTTP response processing.</p>
 * 
 * <p>The primary purpose of this filter is to log the absolute URL path of each
 * incoming request at the DEBUG log level, providing visibility into the request
 * routing and helping with request tracking and debugging.</p>
 * 
 * @see ContainerResponseFilter
 * @see ContainerRequestContext
 * @see ContainerResponseContext
 */
@Provider
public class URLPathListener implements ContainerResponseFilter {

	public final static Logger LOG = LoggerFactory.getLogger(URLPathListener.class);

	@Context
	UriInfo info;

	@Context
	HttpServerRequest request;

	/**
	 * Filters the response and logs the absolute path of the incoming request.
	 * 
	 * <p>This method is called for each HTTP request after the request has been
	 * processed but before the response is sent back to the client. It extracts
	 * the absolute path of the request using {@link ContainerRequestContext}
	 * and logs it at the DEBUG log level.</p>
	 * 
	 * @param requestContext The context of the request being processed
	 * @param responseContext The context of the response to be sent
	 * @throws IOException if an I/O error occurs during filtering
	 */
	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
			throws IOException {
		LOG.debug("URL Request : {}", requestContext.getUriInfo().getAbsolutePath());
	}
}
