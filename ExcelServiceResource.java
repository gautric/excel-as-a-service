package net.a.g.excel.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.a.g.excel.ExcelEngine;

@Path("/totot")
public class ExcelServiceResource {

	public final static Logger LOG = LoggerFactory.getLogger(ExcelServiceResource.class);

	@Inject
	ExcelEngine engine;
	
	static {
		LOG.debug("tototo");
	}

	@POST
	@Consumes({ "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/vnd.ms-excel" })
	public void add(String title, File file) {
		try {
			engine.add(title, new FileInputStream(file));
		} catch (FileNotFoundException ex) {
			LOG.error(null, ex);
		}
		engine.listOfSheet(title);
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Set<String> listOfFile() {
		return engine.list();
	}

	@GET
	@Path("{title}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response listOfSheet(@PathParam("title") String title) {
		if (!engine.title(title)) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
		return Response.status(Response.Status.OK).entity(engine.listOfSheet(title)).build();
	}

	@GET
	@Path("{title}/{sheet}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response sheet(@PathParam("title") String title, @PathParam("sheet") String sheet) {
		return Response.status((engine.sheet(title, sheet)) ? Response.Status.OK : Response.Status.NOT_FOUND).build();
	}

	@GET
	@Path("{title}/{sheet}/{cells}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response cellQuery(@PathParam("title") String title, @PathParam("sheet") String sheet,
			@PathParam("cells") String cell, @QueryParam("raw") @DefaultValue("false") boolean format,
			@Context UriInfo info) {

		if (!engine.sheet(title, sheet)) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
		Map<String, List<String>> query = info.getQueryParameters();

		Map ret = engine.cell(title, sheet, cell.split(","), query);

		return Response.status(Response.Status.OK).entity(ret).build();
	}

	@POST
	@Path("{title}/{sheet}/{cells}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes("application/x-www-form-urlencoded")
	public Response cellForm(@PathParam("title") String title, @PathParam("sheet") String sheet,
			@PathParam("cells") String cell, @QueryParam("raw") @DefaultValue("false") boolean format,
			@Context UriInfo info, final MultivaluedMap<String, String> queryurlencoded) {

		if (!engine.sheet(title, sheet)) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		Map<String, List<String>> query = queryurlencoded;

		Map ret = engine.cell(title, sheet, cell.split(","), query);

		return Response.status(Response.Status.OK).entity(ret).build();
	}

	@POST
	@Path("{title}/{sheet}/{cells}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response cellBody(@PathParam("title") String title, @PathParam("sheet") String sheet,
			@PathParam("cells") String cell, @QueryParam("raw") @DefaultValue("false") boolean format,
			@Context UriInfo info, final String jsonBody) {

		if (!engine.sheet(title, sheet)) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		JSONObject body = new JSONObject(jsonBody);

		Map<String, List<String>> query = body.toMap().entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, e -> Arrays.asList(e.getValue().toString())));

		Map ret = engine.cell(title, sheet, cell.split(","), query);

		return Response.status(Response.Status.OK).entity(ret).build();
	}

}
