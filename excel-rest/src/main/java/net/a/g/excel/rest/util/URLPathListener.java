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

@Provider
public class URLPathListener implements ContainerResponseFilter {

	public final static Logger LOG = LoggerFactory.getLogger(URLPathListener.class);

	@Context
	UriInfo info;

	@Context
	HttpServerRequest request;

	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
			throws IOException {
		LOG.debug("URL Request : {}", requestContext.getUriInfo().getAbsolutePath());
	}

}
