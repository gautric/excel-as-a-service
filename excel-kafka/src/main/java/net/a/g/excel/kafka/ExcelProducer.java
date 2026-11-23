package net.a.g.excel.kafka;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.smallrye.reactive.messaging.kafka.Record;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.a.g.excel.model.ExcelRequest;

@ApplicationScoped
public class ExcelProducer {

	public final static Logger LOG = LoggerFactory.getLogger(ExcelProducer.class);

	@Inject
	@Channel("eaas-request-cmd-out")
	Emitter<Record<String, String>> emitter;

	@Inject
	ObjectMapper om;

	/**
	 * Publishes the request on Kafka.
	 *
	 * @throws IllegalStateException when the request cannot be serialized or sent
	 */
	public void sendRequest(ExcelRequest request) {
		try {
			String json = om.writeValueAsString(request);
			LOG.info("Sending {} - {}", request.getUuid(), json);
			emitter.send(Record.of(request.getUuid(), json));
			LOG.debug("Send OK {}", request.getUuid());
		} catch (Exception e) {
			LOG.error("Unable to send request {}", request.getUuid(), e);
			throw new IllegalStateException("Unable to send request " + request.getUuid(), e);
		}
	}
}