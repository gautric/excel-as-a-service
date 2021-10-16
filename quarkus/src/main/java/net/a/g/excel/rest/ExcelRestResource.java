package net.a.g.excel.rest;

import java.io.IOException;
import java.io.InputStream;
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
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.IOUtils;
import org.eclipse.microprofile.openapi.annotations.ExternalDocumentation;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.a.g.excel.engine.ExcelEngine;
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
			@PathParam("cells") String cell, @QueryParam("_global") @DefaultValue("false") boolean global,
			final String jsonBody) {

		if (!getEngine().isSheetExists(title, sheet)) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		JSONObject body = new JSONObject(jsonBody);

		Map<String, List<String>> query = body.toMap().entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, e -> Arrays.asList(e.getValue().toString())));

		Map<String, Object> ret = getEngine().mapOfCellCalculated(title, sheet, cell.split(","), query, global);

		return Response.status(Response.Status.OK).entity(ret).build();
	}

	@POST
	@Path("{resource}/{sheet}/{cells}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes("application/x-www-form-urlencoded")
	public Response cellForm(@PathParam("resource") String title, @PathParam("sheet") String sheet,
			@PathParam("cells") String cell,  @QueryParam("_global") @DefaultValue("false") boolean global,
			final MultivaluedMap<String, String> queryurlencoded) {

		if (!getEngine().isSheetExists(title, sheet)) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		Map<String, List<String>> query = queryurlencoded;

		Map<String, Object> ret = getEngine().mapOfCellCalculated(title, sheet, cell.split(","), query, global);

		return Response.status(Response.Status.OK).entity(ret).build();
	}

	@GET
	@Path("{resource}/{sheet}/{cells}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response cellQuery(@PathParam("resource") String resource, @PathParam("sheet") String sheetName,
			@PathParam("cells") String cellNames, @QueryParam("_global") @DefaultValue("false") boolean global) {
		Link link = Link.fromUri(uriInfo.getRequestUri()).rel("self").build();

		if (!getEngine().isSheetExists(resource, sheetName)) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
		Map<String, List<String>> query = uriInfo.getQueryParameters();

		Map<String, Object> entity = getEngine().mapOfCellCalculated(resource, sheetName, cellNames.split(","), query, global);

		((Map<String, Object>) entity).replaceAll((k, v) -> createCellValueResource(k, v, resource, sheetName));

		ExcelResult ret = new ExcelResult(entity.size(), entity);

		if (getConf().returnList()) {
			ret.setResults(entity.values());
		}

		return ExcelRestTool.returnOK(ret, link);
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@APIResponses(value = {
			@APIResponse(responseCode = "200", description = "Nominal result, return ExcelResult + ExcelResource[]", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExcelResult.class))) })
	@Operation(summary = "List of Excel Resources", description = "Retrieves and returns the list of Excel Resources")
	public Response getListOfFile() throws Exception {

		Link link = Link.fromUri(uriInfo.getRequestUri()).rel("self").build();

		Object entity = null;
		if (getConf().returnMap()) {
			entity = getEngine().lisfOfResourceName().stream()
					.collect(Collectors.toMap(k -> k, resource -> createExcelResource(resource)));
		} else {
			entity = getEngine().lisfOfResourceName().stream().map(resource -> createExcelResource(resource));
		}

		ExcelResult ret = new ExcelResult(getEngine().countListOfResource(), entity);

		return ExcelRestTool.returnOK(ret, link);
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

		if (!getEngine().isResourceExists(file)) {
			return Response.status(Response.Status.NOT_FOUND).links(link).build();
		}

		List<String> listOfSheet = getEngine().listOfSheet(file);

		Object entity = null;

		if (getConf().returnMap()) {
			entity = listOfSheet.stream().collect(Collectors.toMap(k -> k, sheet -> createSheetResource(file, sheet)));
		} else {
			entity = listOfSheet.stream().map(sheet -> createSheetResource(file, sheet));
		}
		ExcelResult ret = new ExcelResult(listOfSheet.size(), entity);

		return ExcelRestTool.returnOK(ret, link);
	}

	@POST
	@Path("/{resource}")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	@APIResponses(value = {
			@APIResponse(responseCode = "400", description = "Resource uploaded is not Excel file", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExcelResult.class))),
			@APIResponse(responseCode = "202", description = "Resource is accepted", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExcelResult.class))) })
	@Operation(summary = "Create a new Excel Resource", description = "Create a new Excel Resource")
	public Response uploadFile(@PathParam("resource") String resource, MultipartFormDataInput input)
			throws IOException {

		String fileName = "";
		byte[] bytes = null;

		Map<String, List<InputPart>> uploadForm = input.getFormDataMap();
		List<InputPart> inputParts = uploadForm.get("file");

		for (InputPart inputPart : inputParts) {

			MultivaluedMap<String, String> header = inputPart.getHeaders();
			fileName = getFileName(header);

			InputStream inputStream = inputPart.getBody(InputStream.class, null);

			bytes = IOUtils.toByteArray(inputStream);
		}

		ExcelResource excelResource = new ExcelResource();
		excelResource.setName(resource);
		excelResource.setFile(fileName);
		excelResource.setDoc(bytes);

		if (!getEngine().addResource(excelResource)) {
			return ExcelRestTool.returnKO(Response.Status.BAD_REQUEST,
					"Server cannot accept/recognize format file provided");
		}

		String url = uriInfo.getRequestUri().toString();

		ExcelResult ret = new ExcelResult();
		excelResource.setRef(url);
		ret.setSelf(url);
		ret.setResults(excelResource);

		return Response.accepted(ret).build();
	}

	/**
	 * header sample { Content-Type=[image/png], Content-Disposition=[form-data;
	 * name="file"; filename="filename.extension"] }
	 **/
	// get uploaded filename, is there a easy way in RESTEasy?
	private String getFileName(MultivaluedMap<String, String> header) {

		String[] contentDisposition = header.getFirst("Content-Disposition").split(";");

		for (String filename : contentDisposition) {
			if ((filename.trim().startsWith("filename"))) {

				String[] name = filename.split("=");

				String finalFileName = name[1].trim().replaceAll("\"", "");
				return finalFileName;
			}
		}
		return "unknown";
	}

	@GET
	@Path("{resource}/{sheet}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response sheet(@PathParam("resource") String resource, @PathParam("sheet") String sheetName) {
		Link link = Link.fromUri(uriInfo.getRequestUri()).rel("self").build();
		UriBuilder builder = UriBuilder.fromUri(uriInfo.getBaseUri()).path(ExcelRestResource.class, "cellQuery");

		Response.Status status = Response.Status.NOT_FOUND;
		Map<String, ExcelCell> entity = null;
		ExcelResult ret = null;

		if (!sheetName.contains("!")) {
			if (getEngine().isSheetExists(resource, sheetName)) {
				status = Response.Status.OK;

				entity = getEngine().mapOfCell(resource, sheetName, cell -> true);
				ret = new ExcelResult(entity.size(), entity);

				entity.values().stream()
						.forEach(cell -> cell.setRef(builder.build(resource, sheetName, cell.getAddress()).toString()));

				if (getConf().returnList()) {
					ret.setResults(entity.values());
				}
			} else {
				return Response.status(status).build();
			}
		}
		return ExcelRestTool.returnOK(ret, link);
	}

	private ExcelResource createExcelResource(String resource) {
		return new ExcelResource(resource, UriBuilder.fromUri(uriInfo.getBaseUri())
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

	public ExcelConfiguration getConf() {
		return conf;
	}

	public ExcelEngine getEngine() {
		return engine;
	}

	public UriInfo getUriInfo() {
		return uriInfo;
	}

}
