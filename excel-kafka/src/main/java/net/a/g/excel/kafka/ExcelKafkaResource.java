package net.a.g.excel.kafka;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

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
			@APIResponse(responseCode = "202", description = "Request is accepted", content = @Content(mediaType = "application/json")) })
	@Operation(summary = "Post an request to Kafka Topic", description = "Retrieves and returns the list of Excel Cell")
	public Response cellBody(final ExcelRequest request) {
		producer.sendRequest(request);

		return Response.accepted().build();
	}

}