package net.a.g.excel.rest;

import static net.a.g.excel.rest.ExcelConstants.API;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.a.g.excel.engine.ExcelEngine;
import net.a.g.excel.model.ExcelCell;
import net.a.g.excel.model.ExcelLink;
import net.a.g.excel.model.ExcelModel;
import net.a.g.excel.model.ExcelResource;
import net.a.g.excel.model.ExcelResult;
import net.a.g.excel.model.ExcelSheet;
import net.a.g.excel.util.ExcelConfiguration;
import net.a.g.excel.util.ExcelConstants;

@Path(API)
@OpenAPIDefinition(externalDocs = @ExternalDocumentation(description = "schema", url = ExcelConstants.SCHEMA_URI), info = @Info(version = "1.0", title = "Excel Quarkus"))
public class ExcelRestResource {

	public final static Logger LOG = LoggerFactory.getLogger(ExcelRestResource.class);

	@Inject
	ExcelConfiguration conf;

	@Inject
	ExcelEngine engine;

	@Context
	UriInfo uriInfo;

	private void addLink(ExcelResource er) {

		UriBuilder resourceBuilder = getURIBuilder().path(ExcelRestResource.class, "resources");
		injectLink(resourceBuilder, () -> new String[] {}, "list-of-resource").accept(er);

		UriBuilder selfBuilder = getURIBuilder().path(ExcelRestResource.class, "resource");
		injectLink(selfBuilder, () -> new String[] { er.getName() }, "self").accept(er);
		UriBuilder dwnBuilder = getURIBuilder().path(ExcelRestResource.class, "download");
		injectLink(dwnBuilder, () -> new String[] { er.getName() }, "download",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet").accept(er);
		UriBuilder sheetsBuilder = getURIBuilder().path(ExcelRestResource.class, "listOfSheet");
		injectLink(sheetsBuilder, () -> new String[] { er.getName() }, "list-of-sheet").accept(er);
	}

	private void addLink(String resource, ExcelSheet es) {
		UriBuilder upBuilder = getURIBuilder().path(ExcelRestResource.class, "resource");
		injectLink(upBuilder, () -> new String[] { resource }, "resource").accept(es);

		UriBuilder selfBuilder = getURIBuilder().path(ExcelRestResource.class, "sheet");
		injectLink(selfBuilder, () -> new String[] { resource, es.getName() }, "self").accept(es);
		UriBuilder sheetsBuilder = getURIBuilder().path(ExcelRestResource.class, "listOfCell");
		injectLink(sheetsBuilder, () -> new String[] { resource, es.getName() }, "list-of-cell").accept(es);
	}

	private void addLink(String resource, ExcelCell cell) {
		String sheet = cell.getAddress().split("!")[0];
		String address = cell.getAddress().split("!")[1];

		UriBuilder upBuilder = getURIBuilder().path(ExcelRestResource.class, "resource");
		injectLink(upBuilder, () -> new String[] { resource }, "resource").accept(cell);

		UriBuilder selfBuilder = getURIBuilder().path(ExcelRestResource.class, "sheet");
		injectLink(selfBuilder, () -> new String[] { resource, sheet }, "sheet").accept(cell);

		UriBuilder sheetsBuilder = getURIBuilder().path(ExcelRestResource.class, "cell");
		injectLink(sheetsBuilder, () -> new String[] { resource, sheet, address }, "self").accept(cell);

		if (uriInfo.getQueryParameters().size() > 0) {
			ExcelLink el = new ExcelLink();
			el.setHref(uriInfo.getRequestUri().toString());
			el.setRel("query");
			el.setType(MediaType.APPLICATION_JSON);
			cell.getLinks().add(el);

		}

	}

	@GET
	@Path("{resource}/download")
	@Produces("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
	public Response download(@PathParam("resource") String resource) {
		Link link = Link.fromUri(uriInfo.getRequestUri()).rel("self").build();

		if (!getEngine().isResourceExists(resource)) {
			return Response.status(Response.Status.NOT_FOUND).links(link).build();
		}

		ExcelResource file = getEngine().getResource(resource);

		return Response.ok(file.getDoc()).header("content-disposition", "attachment; filename=" + file.getFile())
				.build();
	}

	@GET
	@Path("")
	@Produces(MediaType.APPLICATION_JSON)
	public Response resources() throws Exception {

		Link link = Link.fromUri(uriInfo.getRequestUri()).rel("self").build();

		Collection<ExcelResource> entity = getEngine().lisfOfResource();

		entity.forEach(e -> e.getLinks().clear());

		entity.forEach(e -> addLink(e));

		ExcelResult ret = new ExcelResult(entity);
		return ExcelRestTool.returnOK(ret, link);
	}

	@GET
	@Path("{resource}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response resource(@PathParam("resource") String resource) {
		Link link = Link.fromUri(uriInfo.getRequestUri()).rel("self").build();

		if (!getEngine().isResourceExists(resource)) {
			return Response.status(Response.Status.NOT_FOUND).links(link).build();
		}

		ExcelResource ret = getEngine().getResource(resource);

		ret.getLinks().clear();
		addLink(ret);

		return Response.ok(ret).links(link).build();
	}

	private UriBuilder getURIBuilder() {
		return UriBuilder.fromUri(uriInfo.getBaseUri() + API);
	}

	@POST
	@Path("/{resource}")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	@APIResponses(value = {
			@APIResponse(responseCode = "400", description = "Resource uploaded is not Excel file", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExcelResult.class))),
			@APIResponse(responseCode = "202", description = "Resource is accepted", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExcelResource.class))) })
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

		ExcelResource ret = new ExcelResource();
		ret.setName(resource);
		ret.setFile(fileName);
		ret.setDoc(bytes);

		if (!getEngine().addResource(ret)) {
			return ExcelRestTool.returnKO(Response.Status.BAD_REQUEST,
					"Server cannot accept/recognize format file provided");
		}

		addLink(ret);

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
	@Path("{resource}/sheets")
	@Produces(MediaType.APPLICATION_JSON)
	public Response listOfSheet(@PathParam("resource") String resource) {
		Link link = Link.fromUri(uriInfo.getRequestUri()).rel("self").build();

		if (!getEngine().isResourceExists(resource)) {
			return Response.status(Response.Status.NOT_FOUND).links(link).build();
		}

		List<ExcelSheet> listOfSheet = getEngine().listOfSheet(resource);

		listOfSheet.forEach(es -> addLink(resource, es));

		ExcelResult ret = new ExcelResult(listOfSheet);

		return ExcelRestTool.returnOK(ret, link);
	}

	@GET
	@Path("{resource}/sheet/{sheet}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response sheet(@PathParam("resource") String resource, @PathParam("sheet") String sheetName) {
		Link link = Link.fromUri(uriInfo.getRequestUri()).rel("self").build();

		ExcelSheet ret = null;

		if (!getEngine().isResourceExists(resource)) {
			return Response.status(Response.Status.NOT_FOUND).links(link).build();
		}

		if (!getEngine().isSheetExists(resource, sheetName)) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		ret = new ExcelSheet(sheetName);

		addLink(resource, ret);

		return Response.status(Response.Status.OK).entity(ret).links(link).build();
	}

	@GET
	@Path("{resource}/sheet/{sheet}/cells")
	@Produces(MediaType.APPLICATION_JSON)
	public Response listOfCell(@PathParam("resource") String resource, @PathParam("sheet") String sheetName) {
		Link link = Link.fromUri(uriInfo.getRequestUri()).rel("self").build();

		List<ExcelCell> entity = null;
		ExcelResult ret = null;

		if (!getEngine().isResourceExists(resource)) {
			return Response.status(Response.Status.NOT_FOUND).links(link).build();
		}

		if (!getEngine().isSheetExists(resource, sheetName)) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		entity = getEngine().listOfCell(resource, sheetName, cell -> true);

		entity.forEach(cell -> addLink(resource, cell));
		ret = new ExcelResult(entity);

		return ExcelRestTool.returnOK(ret, link);
	}

	@GET
	@Path("{resource}/sheet/{sheet}/cells/{cells : .*,.*}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response cellsQuery(@PathParam("resource") String resource, @PathParam("sheet") String sheet,
			@PathParam("cells") String cells, @QueryParam("_global") @DefaultValue("false") boolean global) {

		Map<String, List<String>> param = uriInfo.getQueryParameters();

		Map<String, String> query = param.entrySet().stream()
				.collect(Collectors.toMap(e -> renameFunction(sheet).apply(e.getKey()), kv -> kv.getValue().get(0)));

		return computeCells(resource, sheet, cells, global, query);

	}

	private Response computeCells(String resource, String sheetName, String input, boolean global,
			Map<String, String> query) {
		Link link = Link.fromUri(uriInfo.getRequestUri()).rel("self").build();

		if (!getEngine().isResourceExists(resource)) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		if (!getEngine().isSheetExists(resource, sheetName)) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		List<ExcelCell> entity = getEngine().cellCalculation(resource, sheetName, Arrays.asList(input.split(",")),
				query, global);

		entity.forEach(cell -> addLink(resource, cell));

		return ExcelRestTool.returnOK(new ExcelResult(entity), link);
	}

	@GET
	@Path("{resource}/sheet/{sheet}/cell/{cell}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response cell(@PathParam("resource") String resource, @PathParam("sheet") String sheet,
			@PathParam("cell") String cell, @QueryParam("_global") @DefaultValue("false") boolean global) {

		Map<String, List<String>> param = uriInfo.getQueryParameters();
		Map<String, String> query = param.entrySet().stream()
				.collect(Collectors.toMap(e -> renameFunction(sheet).apply(e.getKey()), kv -> kv.getValue().get(0)));

		if (!getEngine().isResourceExists(resource)) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		if (!getEngine().isSheetExists(resource, sheet)) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		List<ExcelCell> engineRet = null;

		engineRet = getEngine().cellCalculation(resource, Arrays.asList(sheet + "!" + cell), query, global);

		if (engineRet.size() == 0) {

			return Response.status(Status.NOT_FOUND).build();

		} else {
			ExcelCell ret = engineRet.get(0);

			addLink(resource, ret);

			return Response.ok(ret).build();
		}
	}

	private Function<String, String> renameFunction(String sheet) {

		return cn -> cn.contains("!") ? cn : sheet + "!" + cn;
	}

	private Consumer<? super ExcelModel> injectLink(UriBuilder builder, Supplier<String[]> supply, String rel,
			String type) {
		return cell -> {
			ExcelLink el = new ExcelLink();
			el.setHref(builder.build(supply.get()).toString());
			el.setRel(rel);
			el.setType(type);
			cell.getLinks().add(el);
		};
	}

	private Consumer<? super ExcelModel> injectLink(UriBuilder builder, Supplier<String[]> supply, String rel) {
		return injectLink(builder, supply, rel, MediaType.APPLICATION_JSON);
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
