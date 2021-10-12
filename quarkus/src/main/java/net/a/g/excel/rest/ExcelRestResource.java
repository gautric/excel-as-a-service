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

import org.eclipse.microprofile.openapi.annotations.ExternalDocumentation;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.a.g.excel.ExcelEngine;
import net.a.g.excel.model.ExcelCell;
import net.a.g.excel.model.ExcelResource;
import net.a.g.excel.model.ExcelResult;
import net.a.g.excel.model.ExcelSheet;
import net.a.g.excel.util.ExcelConfiguration;
import net.a.g.excel.util.ExcelConstants;
import net.a.g.excel.util.ExcelUtils;

@Path("/")
@OpenAPIDefinition(externalDocs = @ExternalDocumentation(description = "schema", url = ExcelConstants.SCHEMA_URI), info = @Info(version = "1.0", title = "Excel Quarkus"))
public class ExcelRestResource {

	public final static Logger LOG = LoggerFactory.getLogger(ExcelRestResource.class);

	@Inject
	ExcelConfiguration conf;

	@Inject
	ExcelEngine engine;

	@Context
	UriInfo uriInfo;

	@POST
	@Path("{resource}/{sheet}/{cells}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response cellBody(@PathParam("resource") String title, @PathParam("sheet") String sheet,
			@PathParam("cells") String cell, @QueryParam("raw") @DefaultValue("false") boolean format,
			final String jsonBody) {

		if (!engine.ifSheetExists(title, sheet)) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		JSONObject body = new JSONObject(jsonBody);

		Map<String, List<String>> query = body.toMap().entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, e -> Arrays.asList(e.getValue().toString())));

		Map<String, Object> ret = engine.computeCell(title, sheet, cell.split(","), query);

		return Response.status(Response.Status.OK).entity(ret).build();
	}

	@POST
	@Path("{resource}/{sheet}/{cells}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes("application/x-www-form-urlencoded")
	public Response cellForm(@PathParam("resource") String title, @PathParam("sheet") String sheet,
			@PathParam("cells") String cell, @QueryParam("raw") @DefaultValue("false") boolean format,
			final MultivaluedMap<String, String> queryurlencoded) {

		if (!engine.ifSheetExists(title, sheet)) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		Map<String, List<String>> query = queryurlencoded;

		Map<String, Object> ret = engine.computeCell(title, sheet, cell.split(","), query);

		return Response.status(Response.Status.OK).entity(ret).build();
	}

	@GET
	@Path("{resource}/{sheet}/{cells}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response cellQuery(@PathParam("resource") String resource, @PathParam("sheet") String sheetName,
			@PathParam("cells") String cellNames) {
		Link link = Link.fromUri(uriInfo.getRequestUri()).rel("self").build();

		if (!engine.ifSheetExists(resource, sheetName)) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
		Map<String, List<String>> query = uriInfo.getQueryParameters();

		Map<String, Object> entity = engine.computeCell(resource, sheetName, cellNames.split(","), query);

		((Map<String, Object>) entity).replaceAll((k, v) -> createCellValueResource(k, v, resource, sheetName));

		ExcelResult ret = new ExcelResult(entity.size(), entity);

		if (conf.returnList()) {
			ret.setResults(entity.values());
		}

		return returnOK(ret, link);
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@APIResponses(value = {
			@APIResponse(responseCode = "200", description = "Nominal result, return ExcelResult + ExcelResource[]", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExcelResult.class))) })
	@Operation(summary = "List of Excel Resources", description = "Retrieves and returns the list of Excel Resources")
	public Response getListOfFile() throws Exception {

		Link link = Link.fromUri(uriInfo.getRequestUri()).rel("self").build();

		Object entity = null;
		if (conf.returnMap()) {
			entity = engine.listOfFile().stream()
					.collect(Collectors.toMap(k -> k, resource -> createExcelResource(resource)));
		} else {
			entity = engine.listOfFile().stream().map(resource -> createExcelResource(resource));
		}

		ExcelResult ret = new ExcelResult(engine.countListOfResource(), entity);

		return returnOK(ret, link);
	}

	@GET
	@Path("{resource}")
	@Produces(MediaType.APPLICATION_JSON)
	@APIResponses(value = {
			@APIResponse(responseCode = "404", description = "Resource not Found", content = @Content(mediaType = "application/json")),
			@APIResponse(responseCode = "200", description = "Nominal result, return ExcelResult + ExcelSheet[]", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExcelResult.class))) })
	@Operation(summary = "List of Excel Sheets", description = "Retrieves and returns the list of Excel Sheets")
	public Response listOfSheet(@PathParam("resource") String file) {
		Link link = Link.fromUri(uriInfo.getRequestUri()).rel("self").build();

		if (!engine.title(file)) {
			return Response.status(Response.Status.NOT_FOUND).links(link).build();
		}

		List<String> listOfSheet = engine.listOfSheet(file);

		Object entity = null;

		if (conf.returnMap()) {
			entity = listOfSheet.stream().collect(Collectors.toMap(k -> k, sheet -> createSheetResource(file, sheet)));
		} else {
			entity = listOfSheet.stream().map(sheet -> createSheetResource(file, sheet));
		}
		ExcelResult ret = new ExcelResult(listOfSheet.size(), entity);

		return returnOK(ret, link);
	}

	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.TEXT_PLAIN)
	public Response sendMultipartData(@MultipartForm MultipartBody data) {

		JSONObject ret = new JSONObject();

		if (conf.isReadOnly()) {
			ret.accumulate("_error", "Service is on readonly mode, you cannot upload file");
			return Response.status(Response.Status.METHOD_NOT_ALLOWED).entity(ret.toString()).build();
		}

		if (!engine.addFile(data.name, data.is)) {
			ret.accumulate("_error", "Server cannot accept/recognize format file provided");
			return Response.status(Status.BAD_REQUEST).entity(ret.toString()).build();
		}

		ret.accumulate(data.name, UriBuilder.fromUri(uriInfo.getRequestUri())
				.path(ExcelRestResource.class, "listOfSheet").build(data.name).toString());

		return Response.accepted(ret.toString()).build();
	}

	@GET
	@Path("{resource}/{sheet}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response sheet(@PathParam("resource") String resource, @PathParam("sheet") String sheetName) {
		Link link = Link.fromUri(uriInfo.getRequestUri()).rel("self").build();

		Response.Status status = Response.Status.NOT_FOUND;
		Map<String, Object> entity = null;
		ExcelResult ret = null;

		if (!sheetName.contains("!")) {
			if (engine.ifSheetExists(resource, sheetName)) {
				status = Response.Status.OK;

				entity = engine.cellFormular(resource, sheetName);
				ret = new ExcelResult(entity.size(), entity);

				((Map<String, Object>) entity)
						.replaceAll((k, f) -> createCellFormulaResource(k, (String) f, resource, sheetName));

				if (conf.returnList()) {
					ret.setResults(entity.values());
				}
			}
		}
		return returnOK(ret, link);
	}

	private ExcelResource createExcelResource(String resource) {
		return new ExcelResource(resource, UriBuilder.fromUri(uriInfo.getRequestUri())
				.path(ExcelRestResource.class, "listOfSheet").build(resource).toString());
	}

	private ExcelSheet createSheetResource(String resource, String sheet) {
		return new ExcelSheet(sheet, UriBuilder.fromUri(uriInfo.getBaseUri()).path(ExcelRestResource.class, "sheet")
				.build(resource, sheet).toString());
	}

	private ExcelCell createCellFormulaResource(String adress, String formula, String file, String sheetName) {
		return new ExcelCell(adress, (String) formula, null, UriBuilder.fromUri(uriInfo.getBaseUri())
				.path(ExcelRestResource.class, "cellQuery").build(file, sheetName, adress).toString(), null);
	}

	private ExcelCell createCellValueResource(String adress, Object value, String file, String sheetName) {

		if (ExcelUtils.checkFullAdress(adress)) {
			return new ExcelCell(adress, null, value,
					UriBuilder.fromUri(uriInfo.getBaseUri()).path(ExcelRestResource.class, "cellQuery")
							.build(file, adress.replaceAll("!.*", ""), adress.replaceAll(".*!", "")).toString(),
					null);
		} else {
			return new ExcelCell(
					adress, null, value, UriBuilder.fromUri(uriInfo.getBaseUri())
							.path(ExcelRestResource.class, "cellQuery").build(file, sheetName, adress).toString(),
					null);
		}
	}

	private Response returnOK(ExcelResult ret, Link link) {
		ret.setSelf(link.getUri().toString());
		return Response.status(Response.Status.OK).entity(ret).links(link).build();
	}
}
