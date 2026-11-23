package net.a.g.excel.kafka;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import net.a.g.excel.model.ExcelError;
import net.a.g.excel.model.ExcelRequest;

@Path("/kafka")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ExcelKafkaResource {

	@Inject
	ExcelProducer producer;

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@APIResponses(value = {
			@APIResponse(responseCode = "202", description = "Request is accepted", content = @Content(mediaType = "application/json")),
			@APIResponse(responseCode = "503", description = "Kafka is unavailable", content = @Content(mediaType = "application/json")) })
	@Operation(summary = "Post an request to Kafka Topic", description = "Retrieves and returns the list of Excel Cell")
	public Response cellBody(final ExcelRequest request) {
		try {
			producer.sendRequest(request);
		} catch (IllegalStateException e) {
			ExcelError error = new ExcelError();
			error.setCode("503");
			error.setError("Request could not be sent to Kafka");
			return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(error).build();
		}
		return Response.accepted().build();
	}

}