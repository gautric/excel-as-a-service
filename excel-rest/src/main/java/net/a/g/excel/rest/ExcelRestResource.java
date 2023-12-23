package net.a.g.excel.rest;

import static net.a.g.excel.rest.ExcelConstants.API;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.PathSegment;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import net.a.g.excel.engine.ExcelEngine;
import net.a.g.excel.model.ExcelCell;
import net.a.g.excel.model.ExcelError;
import net.a.g.excel.model.ExcelLink;
import net.a.g.excel.model.ExcelModel;
import net.a.g.excel.model.ExcelRequest;
import net.a.g.excel.model.ExcelResource;
import net.a.g.excel.model.ExcelResult;
import net.a.g.excel.model.ExcelSheet;
import net.a.g.excel.util.ExcelConfiguration;
import net.a.g.excel.util.ExcelConstants;

@Path(API)
@OpenAPIDefinition(externalDocs = @ExternalDocumentation(description = "schema", url = ExcelConstants.SCHEMA_URI), info = @Info(version = "1.0", title = "Excel As A Service"))
public class ExcelRestResource {

	public final static Logger LOG = LoggerFactory.getLogger(ExcelRestResource.class);

	@Inject
	ExcelConfiguration conf;

	@Inject
	ExcelEngine engine;

	@Context
	UriInfo uriInfo;

	@Inject
	Instance<ExcelResult> result;

	private void addLink(ExcelResource er) {

		UriBuilder resourceBuilder = getURIBuilder().path(ExcelRestResource.class, "resources");
		injectLink(er, resourceBuilder, () -> new String[] {}, "list-of-resource");

		UriBuilder selfBuilder = getURIBuilder().path(ExcelRestResource.class, "resource");
		injectLink(er, selfBuilder, () -> new String[] { er.getName() }, "self");

		UriBuilder dwnBuilder = getURIBuilder().path(ExcelRestResource.class, "download");
		injectLink(er, dwnBuilder, () -> new String[] { er.getName() }, "download",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

		UriBuilder sheetsBuilder = getURIBuilder().path(ExcelRestResource.class, "listOfSheet");
		injectLink(er, sheetsBuilder, () -> new String[] { er.getName() }, "list-of-sheet");
	}

	private void addLink(String resource, ExcelSheet es) {
		UriBuilder upBuilder = getURIBuilder().path(ExcelRestResource.class, "resource");
		injectLink(es, upBuilder, () -> new String[] { resource }, "resource");

		UriBuilder selfBuilder = getURIBuilder().path(ExcelRestResource.class, "sheet");
		injectLink(es, selfBuilder, () -> new String[] { resource, es.getName() }, "self");

		UriBuilder sheetsBuilder = getURIBuilder().path(ExcelRestResource.class, "listOfCell");
		injectLink(es, sheetsBuilder, () -> new String[] { resource, es.getName() }, "list-of-cell");

		UriBuilder computeBuilder = getURIBuilder().path(ExcelRestResource.class, "computeDefinition");
		injectLink(es, computeBuilder, () -> new String[] { resource, es.getName() }, "list-of-template");
	}

	private void addLink(String resource, ExcelCell cell) {
		String sheet = cell.getAddress().split("!")[0];
		String address = cell.getAddress().split("!")[1];

		UriBuilder upBuilder = getURIBuilder().path(ExcelRestResource.class, "resource");
		injectLink(cell, upBuilder, () -> new String[] { resource }, "resource");

		UriBuilder selfBuilder = getURIBuilder().path(ExcelRestResource.class, "sheet");
		injectLink(cell, selfBuilder, () -> new String[] { resource, sheet }, "sheet");

		UriBuilder sheetsBuilder = getURIBuilder().path(ExcelRestResource.class, "cell");
		injectLink(cell, sheetsBuilder, () -> new String[] { resource, sheet, address }, "self");

		if (uriInfo.getQueryParameters().size() > 0) {
			ExcelLink el = new ExcelLink();
			el.setHref(uriInfo.getRequestUri().toString());
			el.setRel("query");
			el.setType(MediaType.APPLICATION_JSON);
			cell.getLinks().add(el);

		}

	}

	@GET
	@Path("{resource}/_download")
	@Produces(value = { "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
			MediaType.APPLICATION_JSON })
	@APIResponses(value = {
			@APIResponse(responseCode = "404", description = "Excel Resource not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExcelResult.class))),
			@APIResponse(responseCode = "200", description = "Excel Resource into OpenXmlFormats ", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExcelResult.class))) })
	@Operation(summary = "Retrieve the Excel Resource full file", description = "The Excel Resource into OpenXmlFormats standard (binary file)")
	public Response download(@PathParam("resource") String resource) {
		Link link = Link.fromUri(uriInfo.getRequestUri()).rel("self").build();

		if (!getEngine().isResourceExists(resource)) {
			return resourceNotFound(resource, link);
		}

		ExcelResource file = getEngine().getResource(resource);

		return Response.ok(file.getDoc()).header("content-disposition", "attachment; filename=" + file.getFile())
				.build();
	}

	private Response resourceNotFound(String resource, Link link) {
		ExcelError ee = new ExcelError();
		ee.setCode("" + Response.Status.NOT_FOUND.getStatusCode());
		ee.setError("Resource '" + resource + "' Not Found");

		return Response.status(Response.Status.NOT_FOUND).type(MediaType.APPLICATION_JSON).links(link).entity(ee)
				.build();
	}

	@GET
	@Path("")
	@Produces(MediaType.APPLICATION_JSON)
	@APIResponses(value = {
			@APIResponse(responseCode = "200", description = "List of Excel Resources", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExcelResult.class))) })
	@Operation(summary = "Retrieve the list of Excel Resources", description = "List of Excel Resources")
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
	@APIResponses(value = {
			@APIResponse(responseCode = "404", description = "Excel Resource not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExcelResult.class))),
			@APIResponse(responseCode = "200", description = "Excel Resource", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExcelResource.class))) })
	@Operation(summary = "Retrieve the Excel Resource", description = "The Excel Resource")
	public Response resource(@PathParam("resource") String resource) {
		Link link = Link.fromUri(uriInfo.getRequestUri()).rel("self").build();

		if (!getEngine().isResourceExists(resource)) {
			return resourceNotFound(resource, link);
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
	@Path("{resource}")
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
	@APIResponses(value = {
			@APIResponse(responseCode = "404", description = "Excel Resource not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExcelResult.class))),
			@APIResponse(responseCode = "200", description = "List of Sheets for the Excel Resource", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExcelResult.class))) })
	@Operation(summary = "Retrieve the list of Sheets for the Excel Resource", description = "List of Sheets for the Excel Resource")
	public Response listOfSheet(@PathParam("resource") String resource) {
		Link link = Link.fromUri(uriInfo.getRequestUri()).rel("self").build();

		if (!getEngine().isResourceExists(resource)) {
			return resourceNotFound(resource, link);
		}

		List<ExcelSheet> listOfSheet = getEngine().listOfSheet(resource);

		listOfSheet.forEach(es -> addLink(resource, es));

		ExcelResult ret = new ExcelResult(listOfSheet);

		return ExcelRestTool.returnOK(ret, link);
	}

	@GET
	@Path("{resource}/sheet/{sheet}")
	@Produces(MediaType.APPLICATION_JSON)
	@APIResponses(value = {
			@APIResponse(responseCode = "404", description = "Sheet or Excel Resource not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExcelResult.class))),
			@APIResponse(responseCode = "200", description = "Sheet of Excel Resource", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExcelSheet.class))) })
	@Operation(summary = "Retrieve the Sheet of Excel Resource", description = "The Sheet of Excel Resource")
	public Response sheet(@PathParam("resource") String resource, @PathParam("sheet") String sheetName) {
		Link link = Link.fromUri(uriInfo.getRequestUri()).rel("self").build();

		ExcelSheet ret = null;

		if (!getEngine().isResourceExists(resource)) {
			return resourceNotFound(resource, link);
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
	@APIResponses(value = {
			@APIResponse(responseCode = "404", description = "Sheet or Excel Resource not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExcelResult.class))),
			@APIResponse(responseCode = "200", description = "List of Cells for the sheet", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExcelResult.class))) })
	@Operation(summary = "Retrieve the list of Cells for the sheet", description = "List of Cells for the sheet")
	public Response listOfCell(@PathParam("resource") String resource, @PathParam("sheet") String sheetName) {
		Link link = Link.fromUri(uriInfo.getRequestUri()).rel("self").build();

		List<ExcelCell> entity = null;
		ExcelResult ret = null;

		if (!getEngine().isResourceExists(resource)) {
			return resourceNotFound(resource, link);
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
	@Path("{resource}/sheet/{sheet}/compute/{output}/{input: .*}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response compute(@PathParam("resource") String resource, @PathParam("sheet") String sheetName,
			@PathParam("output") String output, @PathParam("input") List<PathSegment> input) {
		Link link = Link.fromUri(uriInfo.getRequestUri()).rel("self").build();

		List<String> pullParam = List.of(output.split(","));

		if (!getEngine().isResourceExists(resource)) {
			return resourceNotFound(resource, link);
		}

		if (!getEngine().isSheetExists(resource, sheetName)) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		if (input.size() % 2 != 0) {
			return Response.status(Response.Status.BAD_REQUEST).entity("input path should be even").build();
		}

		Map<String, List<ExcelCell>> mapOfCell = getEngine().listOfAPI(resource, sheetName).stream()
				.collect(Collectors.groupingBy(v -> (v.getMetadata().contains("@input")) ? "IN" : "OUT"));

		String in = mapOfCell.get("IN").stream()
				.map(v -> "/" + extract(v.getMetadata()) + "/{" + extract(v.getMetadata()) + "}")
				.collect(Collectors.joining("")).replaceFirst("/", "");

		UriBuilder resourceBuilder = getURIBuilder().path(ExcelRestResource.class, "compute");

		Map<String, String> template = pullParam.stream()
				.collect(Collectors.toMap(Function.identity(),
						outP -> URLDecoder.decode(
								resourceBuilder.buildFromEncoded(resource, sheetName, outP, in).toString(),
								StandardCharsets.UTF_8),
						(n, o) -> o));

		LOG.debug("Template {}", template);

		Map<String, ExcelCell> inputParam = mapOfCell.get("IN").stream()
				.collect(Collectors.toMap(v -> extract(v.getMetadata()), Function.identity()));
		Map<String, ExcelCell> outputParam = mapOfCell.get("OUT").stream()
				.collect(Collectors.toMap(v -> extract(v.getMetadata()), Function.identity()));

		Map<String, String> injectParam = new HashMap<>();
		for (int i = 0; i < input.size() / 2; i++) {
			String param = input.get(i * 2).getPath();
			String value = input.get(i * 2 + 1).getPath();

			ExcelCell cell = inputParam.get(param);
			if (cell != null) {
				LOG.debug("Add param '{}' -> {} = '{}' ", param, cell.getAddress(), value);
				injectParam.put(cell.getAddress(), value);
			} else {
				LOG.error("Param '{}' = '{}' is not found, skipped", param, value);
			}
		}

		pullParam = (List<String>) pullParam.stream().map(outputParam::get).flatMap(Stream::ofNullable)
				.map(ExcelCell::getAddress).collect(Collectors.toList());

		List<ExcelCell> entity = getEngine().cellCalculation(resource, pullParam, injectParam, false);

		entity.forEach(cell -> {

			ExcelLink el = new ExcelLink();
			el.setHref(template.get(extract(cell.getMetadata())));
			el.setRel("uri-template");
			el.setType(MediaType.APPLICATION_JSON);
			cell.getLinks().add(el);

		});

		ExcelResult er = new ExcelResult(entity);

		UriBuilder selfBuilder = getURIBuilder().path(ExcelRestResource.class, "sheet");
		injectLink(er, selfBuilder, () -> new String[] { resource, sheetName }, "sheet");

		return ExcelRestTool.returnOK(er, link);

	}

	@GET
	@Path("{resource}/sheet/{sheet}/compute")
	@Produces(MediaType.APPLICATION_JSON)
	public Response computeDefinition(@PathParam("resource") String resource, @PathParam("sheet") String sheet) {
		Link link = Link.fromUri(uriInfo.getRequestUri()).rel("self").build();

		
		if (!getEngine().isResourceExists(resource)) {
			return resourceNotFound(resource, link);
		}

		if (!getEngine().isSheetExists(resource, sheet)) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		Map<String, List<ExcelCell>> mapOfCell = getEngine().mapOfAPI(resource, sheet);

		String in = mapOfCell.getOrDefault("IN", List.of()).stream()
				.map(v -> "/" + extract(v.getMetadata()) + "/{" + extract(v.getMetadata()) + "}")
				.collect(Collectors.joining("")).replaceFirst("/", "");

		UriBuilder resourceBuilder = getURIBuilder().path(ExcelRestResource.class, "compute");

		Map<String, String> template = mapOfCell.getOrDefault("OUT", List.of()).stream().map(ExcelCell::getMetadata)
				.map(this::extract)
				.collect(Collectors.toMap(Function.identity(),
						outP -> URLDecoder.decode(
								resourceBuilder.buildFromEncoded(resource, sheet, outP, in).toString(),
								StandardCharsets.UTF_8)));

		LOG.debug("Template {}", template);

		ExcelResult er = new ExcelResult();

		template.entrySet().stream().forEach(kv -> {
			ExcelLink el = new ExcelLink();
			el.setHref(kv.getValue());
			el.setRel("uri-template-" + kv.getKey().toLowerCase());
			el.setType(MediaType.APPLICATION_JSON);
			er.getLinks().add(el);
		});

		UriBuilder selfBuilder = getURIBuilder().path(ExcelRestResource.class, "sheet");
		injectLink(er, selfBuilder, () -> new String[] { resource, sheet }, "sheet");

		return ExcelRestTool.returnOK(er, link);
	}

	@POST
	@Path("{resource}/sheet/{sheet}/compute")
	@Produces(MediaType.APPLICATION_JSON)
	public Response computePOST(@PathParam("resource") String resource, @PathParam("sheet") String sheet,
			final ExcelRequest request) {

		List<String> pullParam = request.getOutputs();

		Link link = Link.fromUri(uriInfo.getRequestUri()).rel("self").build();

		
		if (!getEngine().isResourceExists(resource)) {
			return resourceNotFound(resource, link);
		}

		if (!getEngine().isSheetExists(resource, sheet)) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		Map<String, List<ExcelCell>> mapOfCell = getEngine().mapOfAPI(resource, sheet);

		Map<String, ExcelCell> inputParam = mapOfCell.get("IN").stream()
				.collect(Collectors.toMap(v -> extract(v.getMetadata()), Function.identity()));
		Map<String, ExcelCell> outputParam = mapOfCell.get("OUT").stream()
				.collect(Collectors.toMap(v -> extract(v.getMetadata()), Function.identity()));

		Map<String, String> injectParam = new HashMap<>();
		for (Map.Entry<String, String> entry : request.getInputs().entrySet()) {
			String param = entry.getKey();
			String value = entry.getValue();

			ExcelCell cell = inputParam.get(param);
			if (cell != null) {
				LOG.debug("Add param '{}' -> {} = '{}' ", param, cell.getAddress(), value);
				injectParam.put(cell.getAddress(), value);
			} else {
				LOG.error("Param '{}' = '{}' is not found, skipped", param, value);
			}
		}

		pullParam = (List<String>) pullParam.stream().map(p -> outputParam.get(p)).map(ExcelCell::getAddress)
				.collect(Collectors.toList());

		List<ExcelCell> entity = getEngine().cellCalculation(resource, sheet, pullParam, injectParam, false);

		return ExcelRestTool.returnOK(new ExcelResult(entity), link);

	}

	private String extract(String input) {
		Pattern p = Pattern.compile("@(input|output)\\(\"?(?<input>[A-Za-z0-9]+)\"?\\)");
		Matcher m = p.matcher(input);
		while (m.matches()) {
			return m.group("input");
		}
		return null;
	}

	private String mapType(String metadata) {

		switch (metadata) {
		case "BOOLEAN":
			return "true|false";
		case "STRING":
			return "[^/]%2B";
		case "NUMERIC":
			return "[%2B-]?([0-9]*).?[0-9]%2B";
		case "DATE":
			return "\\d{4}\\-(0?[1-9]|1[012])\\-(0?[1-9]|[12][0-9]|3[01])%2A";
		default:
			return "";
		}

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
			return resourceNotFound(resource, link);
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

		Link link = Link.fromUri(uriInfo.getRequestUri()).rel("self").build();

		if (!getEngine().isResourceExists(resource)) {
			return resourceNotFound(resource, link);
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

	private void injectLink(ExcelModel obj, UriBuilder builder, Supplier<String[]> supply, String rel) {
		injectLink(obj, builder, supply, rel, MediaType.APPLICATION_JSON);

	}

	private void injectLink(ExcelModel obj, UriBuilder builder, Supplier<String[]> supply, String rel, String type) {
		ExcelLink el = new ExcelLink();
		el.setHref(builder.build(supply.get()).toString());
		el.setRel(rel);
		el.setType(type);
		obj.getLinks().add(el);
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
