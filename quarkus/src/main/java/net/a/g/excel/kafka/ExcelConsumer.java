package net.a.g.excel.kafka;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.smallrye.reactive.messaging.kafka.Record;
import net.a.g.excel.engine.ExcelEngine;
import net.a.g.excel.model.ExcelCell;
import net.a.g.excel.model.ExcelRequest;

@ApplicationScoped
public class ExcelConsumer {

	public final static Logger LOG = LoggerFactory.getLogger(ExcelConsumer.class);

	@Inject
	ExcelEngine engine;

	@Inject
	ObjectMapper om;

	@Incoming("eaas-in")
	public void receive(Record<String, String> record) {
		LOG.info("Received {} - {}", record.key(), record.value());

		try {
			ExcelRequest er = om.readValue(record.value(), ExcelRequest.class);
			List<ExcelCell> reply = engine.cellCalculation(er.getResource(), er.getSheet(), er.getOutputs(),
					er.getInputs(), false);

			LOG.info("{}", om.writeValueAsString(reply));

		} catch (Exception ex) {
			LOG.error("Cannot execute {}", ex);
		}

	}
}