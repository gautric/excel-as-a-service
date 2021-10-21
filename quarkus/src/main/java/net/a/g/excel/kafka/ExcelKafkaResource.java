package net.a.g.excel.kafka;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.json.JSONObject;

import net.a.g.excel.model.ExcelRequest;
import net.a.g.excel.model.ExcelResult;

@Path("/kafka")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ExcelKafkaResource {

	@Inject
	ExcelProducer producer;

	

	@POST
	@Path("{resource}/{sheet}/{cells}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@APIResponses(value = {
			@APIResponse(responseCode = "404", description = "if {resource} or {sheet} is not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExcelResult.class))),
			@APIResponse(responseCode = "200", description = "Nominal result, return ExcelResult + ExcelCell[]", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExcelResult.class))) })
	@Operation(summary = "List of Excel Cell computed", description = "Retrieves and returns the list of Excel Cell")
	public Response cellBody(@PathParam("resource") String resource, @PathParam("sheet") String sheetName,
			@PathParam("cells") String cellNames, @QueryParam("_global") @DefaultValue("false") boolean global,
			final String jsonBody) {
		JSONObject body = new JSONObject(jsonBody);

		Map<String, List<String>> query = body.toMap().entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, e -> Arrays.asList(e.getValue().toString())));

		return computeCells(resource, sheetName, cellNames, global, query);
	}

	@POST
	@Path("{resource}/{sheet}/{cells}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@APIResponses(value = {
			@APIResponse(responseCode = "404", description = "if {resource} or {sheet} is not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExcelResult.class))),
			@APIResponse(responseCode = "200", description = "Nominal result, return ExcelResult + ExcelCell[]", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExcelResult.class))) })
	@Operation(summary = "List of Excel Cell computed", description = "Retrieves and returns the list of Excel Cell")
	public Response cellForm(@PathParam("resource") String resource, @PathParam("sheet") String sheetName,
			@PathParam("cells") String cellNames, @QueryParam("_global") @DefaultValue("false") boolean global,
			final MultivaluedMap<String, String> queryurlencoded) {

		Map<String, List<String>> query = queryurlencoded;

		return computeCells(resource, sheetName, cellNames, global, query);
	}

	private Response computeCells(String resource, String sheetName, String outputs, boolean global,
			Map<String, List<String>> query) {

		ExcelRequest er = new ExcelRequest();
		er.setResource(resource);
		er.setSheet(sheetName);
		er.setOutputs(Arrays.asList(outputs.split(",")));
		er.setInputs(query.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0))));

		
		producer.sendRequest(er);
		
        return Response.accepted().build();
	}

}