package net.a.g.excel.kafka;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.smallrye.reactive.messaging.kafka.Record;
import net.a.g.excel.model.ExcelRequest;

@ApplicationScoped
public class ExcelProducer {

	public final static Logger LOG = LoggerFactory.getLogger(ExcelProducer.class);

	@Inject
	@Channel("eaas-request-cmd-in")
	Emitter<Record<String, String>> emitter;

	@Inject
	ObjectMapper om;

	public void sendRequest(ExcelRequest request) {
		String json = null;
		try {
			json = om.writeValueAsString(request);

			LOG.info("Sending {} - {}", request.getUuid(), json);
			emitter.send(Record.of(request.getUuid(), json));
			LOG.info("Send OK", request.getUuid(), json);
			
		} catch (Exception e) {
		}
	}
}