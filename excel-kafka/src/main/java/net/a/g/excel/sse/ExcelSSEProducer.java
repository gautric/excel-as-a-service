package net.a.g.excel.sse;

import java.io.IOException;
import java.util.Objects;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import net.a.g.excel.model.ExcelResult;

@Path("/sse")
public class ExcelSSEProducer {

	private final Logger LOG = LoggerFactory.getLogger(ExcelSSEProducer.class);

	private OutboundSseEvent.Builder eventBuilder;
	private SseEventSink sseEventSink = null;

	@Inject
	ObjectMapper om;

	@Context
	public void setSse(Sse sse) {
		this.eventBuilder = sse.newEventBuilder();
	}

	@GET
	@Produces(MediaType.SERVER_SENT_EVENTS)
	public void consume(@Context SseEventSink sseEventSink) {
		this.sseEventSink = sseEventSink;
	}

	@Incoming("eaas-response-channel")
	public void onMessage(ExcelResult result) throws IOException {

		String payload = om.writeValueAsString(result);

		LOG.info("Received Result : {} ", payload);

		if (Objects.nonNull(sseEventSink)) {

			OutboundSseEvent sseEvent = this.eventBuilder.name("message").id(result.getUuid())
					.mediaType(MediaType.APPLICATION_JSON_TYPE).data(payload).reconnectDelay(3000).comment(payload)
					.build();
			sseEventSink.send(sseEvent);
		}
	}

}