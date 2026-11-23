package net.a.g.excel.rest;

import static net.a.g.excel.rest.ExcelConstants.API;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.PathSegment;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import net.a.g.excel.engine.ExcelEngine;
import net.a.g.excel.engine.ExcelEngineImpl;
import net.a.g.excel.exception.ExcelEngineException;
import net.a.g.excel.exception.ExcelInvalidInputException;
import net.a.g.excel.validation.InputValidator;
import net.a.g.excel.model.ExcelCell;
import net.a.g.excel.model.ExcelError;
import net.a.g.excel.model.ExcelLink;
import net.a.g.excel.model.ExcelModel;
import net.a.g.excel.model.ExcelRequest;
import net.a.g.excel.model.ExcelResource;
import net.a.g.excel.model.ExcelResult;
import net.a.g.excel.model.ExcelSheet;
import net.a.g.excel.param.ExcelParameter;
import net.a.g.excel.repository.ExcelRepository;
import net.a.g.excel.util.ExcelConstants;

@Path(API)
@OpenAPIDefinition(externalDocs = @ExternalDocumentation(description = "schema", url = ExcelConstants.SCHEMA_URI), info = @Info(version = "1.0", title = "Excel As A Service"))
public class ExcelRestResource {

	public final static Logger LOG = LoggerFactory.getLogger(ExcelRestResource.class);

	@Inject
	ExcelParameter conf;

	@Inject
	ExcelEngine engine;
	
	@Inject
	ExcelRepository repo;

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
	public Response download(@PathParam("resource") String resource, @Context Request request) {
		Link link = Link.fromUri(uriInfo.getRequestUri()).rel("self").build();

		if (!repo.contains(resource)) {
			return resourceNotFound(resource, link);
		}

		ExcelResource file = repo.get(resource);

		// Conditional GET: clients re-downloading an unchanged workbook get a 304.
		EntityTag etag = etag(file);
		Response.ResponseBuilder notModified = request.evaluatePreconditions(etag);
		if (notModified != null) {
			return notModified.tag(etag).build();
		}

		return Response.ok(file.getDoc(), XLSX).tag(etag).cacheControl(revalidate())
				.header("content-disposition", "attachment; filename=\"" + file.getFile() + "\"").build();
	}

	private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

	private static EntityTag etag(ExcelResource file) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(file.getDoc());
			return new EntityTag(HexFormat.of().formatHex(digest, 0, 16));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	private static CacheControl revalidate() {
		CacheControl cc = new CacheControl();
		cc.setNoCache(true);
		return cc;
	}

	private Response resourceNotFound(String resource, Link link) {
		ExcelError ee = new ExcelError();
		ee.setCode("" + Response.Status.NOT_FOUND.getStatusCode());
		ee.setError("Resource '" + resource + "' Not Found");

		return Response.status(Response.Status.NOT_FOUND).type(MediaType.APPLICATION_JSON).links(link).entity(ee)
				.build();
	}
	
	private Response sheetNotFound(String resource, String sheet, Link link) {
		ExcelError ee = new ExcelError();
		ee.setCode("" + Response.Status.NOT_FOUND.getStatusCode());
		ee.setError(" Sheet '" + sheet + "' Not Found in Resource '" + resource + "'");

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

		Collection<ExcelResource> entity = repo.listOfResource();

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

		if (!repo.contains(resource)) {
			return resourceNotFound(resource, link);
		}

		ExcelResource ret = repo.get(resource);

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
	@Operation(summary = "Create a new Excel Resource", description = "Upload a workbook (multipart field 'file'). Answers 201 Created with a Location header")
	public Response uploadFile(@PathParam("resource") String resource, MultipartFormDataInput input)
			throws IOException {
		return store(resource, input);
	}

	@PUT
	@Path("{resource}")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	@APIResponses(value = {
			@APIResponse(responseCode = "400", description = "Resource uploaded is not Excel file"),
			@APIResponse(responseCode = "200", description = "Resource replaced"),
			@APIResponse(responseCode = "201", description = "Resource created") })
	@Operation(summary = "Create or replace an Excel Resource", description = "Idempotent upload (multipart field 'file')")
	public Response putFile(@PathParam("resource") String resource, MultipartFormDataInput input) throws IOException {
		return store(resource, input);
	}

	@DELETE
	@Path("{resource}")
	@APIResponses(value = { @APIResponse(responseCode = "204", description = "Resource deleted"),
			@APIResponse(responseCode = "404", description = "Excel Resource not found") })
	@Operation(summary = "Delete an Excel Resource")
	public Response delete(@PathParam("resource") String resource) {
		if (!repo.remove(resource)) {
			return resourceNotFound(resource, Link.fromUri(uriInfo.getRequestUri()).rel("self").build());
		}
		return Response.noContent().build();
	}

	private Response store(String resource, MultipartFormDataInput input) throws IOException {
		List<InputPart> inputParts = input == null ? null : input.getFormDataMap().get("file");
		if (inputParts == null || inputParts.isEmpty()) {
			return ExcelRestTool.error(Status.BAD_REQUEST, "Multipart field 'file' is required");
		}

		InputPart part = inputParts.get(0);
		byte[] bytes;
		try (InputStream inputStream = part.getBody(InputStream.class, null)) {
			bytes = IOUtils.toByteArray(inputStream);
		}

		ExcelResource ret = new ExcelResource();
		ret.setName(resource);
		ret.setFile(getFileName(part.getHeaders()));
		ret.setDoc(bytes);

		boolean existed = repo.contains(resource);
		boolean added;
		try {
			added = repo.add(ret);
		} catch (ExcelInvalidInputException e) {
			throw e;
		} catch (ExcelEngineException e) {
			added = false;
		}
		if (!added) {
			return ExcelRestTool.error(Status.BAD_REQUEST, "Server cannot accept/recognize format file provided");
		}

		addLink(ret);

		if (existed) {
			return Response.ok(ret).build();
		}
		return Response.created(getURIBuilder().path(ExcelRestResource.class, "resource").build(resource))
				.entity(ret).build();
	}

	/**
	 * header sample { Content-Type=[image/png], Content-Disposition=[form-data;
	 * name="file"; filename="filename.extension"] }
	 **/
	// get uploaded filename, is there a easy way in RESTEasy?
	private String getFileName(MultivaluedMap<String, String> header) {

		String disposition = header.getFirst("Content-Disposition");
		if (disposition == null) {
			return "unknown";
		}
		String[] contentDisposition = disposition.split(";");

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

		if (!repo.contains(resource)) {
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

		if (!repo.contains(resource)) {
			return resourceNotFound(resource, link);
		}

		if (!getEngine().isSheetExists(resource, sheetName)) {
			return sheetNotFound(resource, sheetName, link);
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

		if (!repo.contains(resource)) {
			return resourceNotFound(resource, link);
		}

		if (!getEngine().isSheetExists(resource, sheetName)) {
			return sheetNotFound(resource, sheetName, link);
		}

		entity = getEngine().listOfCell(resource, sheetName, cell -> true);

		entity.forEach(cell -> addLink(resource, cell));
		ret = new ExcelResult(entity);

		return ExcelRestTool.returnOK(ret, link);
	}

	@GET
	@Path("{resource}/sheet/{sheet}/compute/{output}/{input: .*}")
	@Produces(MediaType.APPLICATION_JSON)
	@APIResponses(value = {
			@APIResponse(responseCode = "404", description = "Sheet or Excel Resource not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExcelResult.class))),
			@APIResponse(responseCode = "200", description = "List of Cells for the sheet", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExcelResult.class))) })
	@Operation(summary = "Compute", description = "Compute")
	public Response compute(@PathParam("resource") String resource, @PathParam("sheet") String sheetName,
			@PathParam("output") String output, @PathParam("input") List<PathSegment> input) {
		Link link = Link.fromUri(uriInfo.getRequestUri()).rel("self").build();

		List<String> pullParam = List.of(output.split(","));

		if (!repo.contains(resource)) {
			return resourceNotFound(resource, link);
		}

		if (!getEngine().isSheetExists(resource, sheetName)) {
			return sheetNotFound(resource, sheetName, link);
		}

		if (input.size() % 2 != 0) {
			return ExcelRestTool.error(Status.BAD_REQUEST,
					"Input path should have an even number of segments (name/value pairs)");
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("Log input xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
			LOG.debug("resource : {}",resource);
			LOG.debug("sheet : {}", sheetName);
			LOG.debug("output : {}",output);
			LOG.debug("input : {}",input);
			LOG.debug("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");

		}
		
		Map<String, List<ExcelCell>> mapOfCell = getEngine().mapOfAPI(resource, sheetName);

		String in = inputTemplate(mapOfCell);

		UriBuilder resourceBuilder = getURIBuilder().path(ExcelRestResource.class, "compute");

		Map<String, String> template = pullParam.stream()
				.collect(Collectors.toMap(Function.identity(),
						outP -> URLDecoder.decode(
								resourceBuilder.buildFromEncoded(resource, sheetName, outP, in).toString(),
								StandardCharsets.UTF_8),
						(n, o) -> o));

		LOG.debug("Template {}", template);

		// Path inputs keep their historical leniency: unknown names are skipped.
		java.util.Set<String> known = mapOfCell.getOrDefault("IN", List.of()).stream()
				.map(v -> extract(v.getMetadata())).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
		Map<String, String> injectParam = new LinkedHashMap<>();
		for (int i = 0; i < input.size() / 2; i++) {
			String param = input.get(i * 2).getPath();
			String value = input.get(i * 2 + 1).getPath();
			if (known.contains(param)) {
				injectParam.put(param, value);
			} else {
				LOG.warn("Param '{}' = '{}' is not found, skipped", param, value);
			}
		}

		ExcelRequest request = new ExcelRequest();
		request.setResource(resource);
		request.setSheet(sheetName);
		request.setOutputs(pullParam);
		request.setInputs(injectParam);

		List<ExcelCell> entity = List.copyOf(getEngine().compute(request).getCells());

		entity.forEach(cell -> {
			String href = template.get(extract(cell.getMetadata()));
			if (href == null) {
				return;
			}
			ExcelLink el = new ExcelLink();
			el.setHref(href);
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
	@APIResponses(value = {
			@APIResponse(responseCode = "404", description = "Sheet or Excel Resource not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExcelResult.class))),
			@APIResponse(responseCode = "200", description = "List of Cells for the sheet", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExcelResult.class))) })
	@Operation(summary = "Compute", description = "Compute")
	public Response computeDefinition(@PathParam("resource") String resource, @PathParam("sheet") String sheet) {
		Link link = Link.fromUri(uriInfo.getRequestUri()).rel("self").build();

		
		if (!repo.contains(resource)) {
			return resourceNotFound(resource, link);
		}

		if (!getEngine().isSheetExists(resource, sheet)) {
			return sheetNotFound(resource, sheet, link);
		}

		Map<String, List<ExcelCell>> mapOfCell = getEngine().mapOfAPI(resource, sheet);

		String in = inputTemplate(mapOfCell);

		UriBuilder resourceBuilder = getURIBuilder().path(ExcelRestResource.class, "compute");

		Map<String, String> template = mapOfCell.getOrDefault("OUT", List.of()).stream().map(ExcelCell::getMetadata)
				.map(this::extract)
				.filter(java.util.Objects::nonNull)
				.distinct()
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
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@APIResponses(value = {
			@APIResponse(responseCode = "200", description = "Computed cells", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExcelResult.class))),
			@APIResponse(responseCode = "400", description = "Unknown input/output or invalid value"),
			@APIResponse(responseCode = "404", description = "Sheet or Excel Resource not found") })
	@Operation(summary = "Compute (JSON body)", description = "Body: {\"inputs\": {\"PEP\": \"true\"}, \"outputs\": [\"SCORE\"]}. "
			+ "Names are @input/@output names or cell references (B2, Sheet!B2). No outputs = every @output of the sheet")
	public Response computeSheetPOST(@PathParam("resource") String resource, @PathParam("sheet") String sheet,
			ExcelRequest request) {
		ExcelRequest req = request == null ? new ExcelRequest() : request;
		req.setResource(resource);
		req.setSheet(sheet);
		return computeResponse(req);
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("compute")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "Compute (resource and sheet in the JSON body)", description = "Same as POST {resource}/sheet/{sheet}/compute; cross-workbook references enabled unless 'global' is false")
	public Response computePOST(final ExcelRequest request) {
		if (request == null) {
			return ExcelRestTool.error(Status.BAD_REQUEST, "JSON body is required");
		}
		if (request.getGlobal() == null) {
			request.setGlobal(Boolean.TRUE); // historical behaviour of this endpoint
		}
		return computeResponse(request);
	}

	private Response computeResponse(ExcelRequest request) {
		LOG.debug("{}", request);
		Link link = Link.fromUri(uriInfo.getRequestUri()).rel("self").build();
		ExcelResult result = getEngine().compute(request);
		return ExcelRestTool.returnOK(result, link);
	}

	private String extract(String input) {
		return ExcelEngineImpl.apiName(input);
	}

	/** "PEP/{PEP}/COUNTRY/{COUNTRY}" built from the @input cells of the sheet. */
	private String inputTemplate(Map<String, List<ExcelCell>> mapOfCell) {
		return mapOfCell.getOrDefault("IN", List.of()).stream().map(v -> extract(v.getMetadata()))
				.filter(java.util.Objects::nonNull).map(n -> "/" + n + "/{" + n + "}").collect(Collectors.joining(""))
				.replaceFirst("/", "");
	}

	@GET
	@Path("{resource}/sheet/{sheet}/cells/{cells : .*,.*}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response cellsQuery(@PathParam("resource") String resource, @PathParam("sheet") String sheet,
			@PathParam("cells") String cells, @QueryParam("_global") @DefaultValue("false") boolean global) {

		Map<String, String> query = queryInputs(sheet);

		return computeCells(resource, sheet, cells, global, query);

	}

	private Response computeCells(String resource, String sheetName, String input, boolean global,
			Map<String, String> query) {
		Link link = Link.fromUri(uriInfo.getRequestUri()).rel("self").build();

		
		if (!repo.contains(resource)) {
			return resourceNotFound(resource, link);
		}

		if (!getEngine().isSheetExists(resource, sheetName)) {
			return sheetNotFound(resource, sheetName, link);
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

		Map<String, String> query = queryInputs(sheet);

		Link link = Link.fromUri(uriInfo.getRequestUri()).rel("self").build();

		if (!repo.contains(resource)) {
			return resourceNotFound(resource, link);
		}

		if (!getEngine().isSheetExists(resource, sheet)) {
			return sheetNotFound(resource, sheet, link);
		}

		try {
			InputValidator.validateCellReference(sheet + "!" + cell);
		} catch (ExcelInvalidInputException e) {
			return ExcelRestTool.error(Status.NOT_FOUND, "Cell '" + cell + "' Not Found in Sheet '" + sheet + "'");
		}

		List<ExcelCell> engineRet = null;

		engineRet = getEngine().cellCalculation(resource, Arrays.asList(sheet + "!" + cell), query, global, false);

		if (engineRet.size() == 0) {

			return ExcelRestTool.error(Status.NOT_FOUND, "Cell '" + cell + "' Not Found in Sheet '" + sheet + "'");

		} else {
			ExcelCell ret = engineRet.get(0);

			addLink(resource, ret);

			return Response.ok(ret).build();
		}
	}

	/** Query parameters as engine inputs; options such as {@code _global} start with '_' and are not cells. */
	private Map<String, String> queryInputs(String sheet) {
		return uriInfo.getQueryParameters().entrySet().stream().filter(e -> !e.getKey().startsWith("_"))
				.collect(Collectors.toMap(e -> renameFunction(sheet).apply(e.getKey()), kv -> kv.getValue().get(0)));
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

	public ExcelParameter getConf() {
		return conf;
	}

	public ExcelEngine getEngine() {
		return engine;
	}

	public UriInfo getUriInfo() {
		return uriInfo;
	}
}
