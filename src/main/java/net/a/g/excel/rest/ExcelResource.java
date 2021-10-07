package net.a.g.excel.rest;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.a.g.excel.ExcelEngine;
import net.a.g.excel.util.ExcelConfiguration;
import net.a.g.excel.util.MultipartBody;

@Path("/")
public class ExcelResource {

	public final static Logger LOG = LoggerFactory.getLogger(ExcelResource.class);

	@Inject
	ExcelConfiguration conf;

	@Inject
	ExcelEngine engine;

	@Context
	UriInfo uriInfo;

	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.TEXT_PLAIN)
	public Response sendMultipartData(@MultipartForm MultipartBody data) {

		JSONObject ret = new JSONObject();

		if (conf.isReadOnly()) {
			ret.accumulate("_error", "Service is on readonly mode, you cannot upload file");
			return Response.status(Response.Status.NOT_ACCEPTABLE).entity(ret.toString()).build();
		}
		
		if(!engine.addFile(data.name, data.is)) {
			ret.accumulate("_error", "Server cannot accept/recognize format file provided");
			return Response.status(Status.BAD_REQUEST).entity(ret.toString()).build();
		}		

		ret.accumulate(data.name, UriBuilder.fromUri(uriInfo.getRequestUri()).path(ExcelResource.class, "listOfSheet")
				.build(data.name).toString());

		return Response.accepted(ret.toString()).build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getListOfFile() throws Exception {

		JSONObject ret = new JSONObject();

		engine.list().stream()
				.map(file -> new String[] { file, UriBuilder.fromUri(uriInfo.getRequestUri())
						.path(ExcelResource.class, "listOfSheet").build(file).toString() })
				.forEach(uri -> ret.accumulate(uri[0], uri[1]));

		Link link = Link.fromUri(uriInfo.getRequestUri()).rel("self").build();

		return Response.ok(ret.toString()).links(link).build();

	}

	@GET
	@Path("{file}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response listOfSheet(@PathParam("file") String file) {
		Link link = Link.fromUri(uriInfo.getRequestUri()).rel("self").build();

		if (!engine.title(file)) {
			return Response.status(Response.Status.NOT_FOUND).links(link).build();
		}

		JSONObject ret = new JSONObject();

		engine.listOfSheet(file).stream()
				.map(sheet -> new String[] { sheet, UriBuilder.fromUri(uriInfo.getBaseUri())
						.path(ExcelResource.class, "sheet").build(file, sheet).toString() })
				.forEach(uri -> ret.accumulate(uri[0], uri[1]));

		return Response.status(Response.Status.OK).entity(ret.toString()).links(link).build();
	}

	@GET
	@Path("{file}/{sheet}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response sheet(@PathParam("file") String file, @PathParam("sheet") String sheetName) {
		Link link = Link.fromUri(uriInfo.getRequestUri()).rel("self").build();

		Response.Status status = Response.Status.NOT_FOUND;
		Object entity = null;

		if (engine.sheet(file, sheetName)) {
			status = Response.Status.OK;
			entity = engine.cellFormular(file, sheetName);
		}

		return Response.status(status).entity(entity).links(link).build();
	}

	@GET
	@Path("{file}/{sheet}/{cells}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response cellQuery(@PathParam("file") String fileName, @PathParam("sheet") String sheetName,
			@PathParam("cells") String cellNames, @QueryParam("raw") @DefaultValue("false") boolean format) {

		if (!engine.sheet(fileName, sheetName)) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
		Map<String, List<String>> query = uriInfo.getQueryParameters();

		Map<String, Object> ret = engine.computeCell(fileName, sheetName, cellNames.split(","), query);

		return Response.status(Response.Status.OK).entity(ret).build();
	}

	@POST
	@Path("{title}/{sheet}/{cells}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes("application/x-www-form-urlencoded")
	public Response cellForm(@PathParam("title") String title, @PathParam("sheet") String sheet,
			@PathParam("cells") String cell, @QueryParam("raw") @DefaultValue("false") boolean format,
			final MultivaluedMap<String, String> queryurlencoded) {

		if (!engine.sheet(title, sheet)) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		Map<String, List<String>> query = queryurlencoded;

		Map<String, Object> ret = engine.computeCell(title, sheet, cell.split(","), query);

		return Response.status(Response.Status.OK).entity(ret).build();
	}

	@POST
	@Path("{title}/{sheet}/{cells}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response cellBody(@PathParam("title") String title, @PathParam("sheet") String sheet,
			@PathParam("cells") String cell, @QueryParam("raw") @DefaultValue("false") boolean format,
			final String jsonBody) {

		if (!engine.sheet(title, sheet)) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		JSONObject body = new JSONObject(jsonBody);

		Map<String, List<String>> query = body.toMap().entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, e -> Arrays.asList(e.getValue().toString())));

		Map<String, Object> ret = engine.computeCell(title, sheet, cell.split(","), query);

		return Response.status(Response.Status.OK).entity(ret).build();
	}

}
