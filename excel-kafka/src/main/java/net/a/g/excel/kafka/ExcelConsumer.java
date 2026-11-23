package net.a.g.excel.kafka;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.smallrye.reactive.messaging.annotations.Blocking;
import io.smallrye.reactive.messaging.annotations.Broadcast;
import io.smallrye.reactive.messaging.kafka.Record;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.a.g.excel.engine.ExcelEngine;
import net.a.g.excel.model.ExcelRequest;
import net.a.g.excel.model.ExcelResult;
import net.a.g.excel.util.ExcelErrors;

/**
 * Consumes compute requests (same JSON as REST {@code POST /eaas/api/compute})
 * and publishes one {@link ExcelResult} per request, correlated by {@code uuid}.
 */
@ApplicationScoped
public class ExcelConsumer {

	public final static Logger LOG = LoggerFactory.getLogger(ExcelConsumer.class);

	@Inject
	ExcelEngine engine;

	@Inject
	ObjectMapper om;

	@Incoming("eaas-request-cmd-in")
	@Broadcast
	@Outgoing("eaas-response-channel")
	@Blocking(ordered = false) // POI evaluation is CPU-bound: keep it off the event loop, in parallel
	public ExcelResult receive(Record<String, String> record) {
		LOG.debug("Received {} - {}", record.key(), record.value());
		ExcelRequest er = null;
		try {
			com.fasterxml.jackson.databind.JsonNode json = om.readTree(record.value());
			if (json == null || !json.isObject()) {
				throw com.fasterxml.jackson.databind.exc.MismatchedInputException.from(null, ExcelRequest.class,
						"Expected a JSON object");
			}
			er = om.treeToValue(json, ExcelRequest.class);
			if (!json.hasNonNull("uuid") && record.key() != null) {
				er.setUuid(record.key()); // keep the request correlated with its Kafka key
			}
			return engine.compute(er);
		} catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
			return ExcelErrors.toResult(new IllegalArgumentException("Invalid JSON request: " + ex.getOriginalMessage()),
					record.key());
		} catch (Exception ex) {
			return ExcelErrors.toResult(ex, er == null ? record.key() : er.getUuid());
		}
	}
}
