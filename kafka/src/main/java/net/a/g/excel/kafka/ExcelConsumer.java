package net.a.g.excel.kafka;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.smallrye.reactive.messaging.annotations.Broadcast;
import io.smallrye.reactive.messaging.kafka.Record;
import net.a.g.excel.engine.ExcelEngine;
import net.a.g.excel.model.ExcelCell;
import net.a.g.excel.model.ExcelError;
import net.a.g.excel.model.ExcelRequest;
import net.a.g.excel.model.ExcelResult;

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
	public ExcelResult receive(Record<String, String> record) {
		LOG.info("Received {} - {}", record.key(), record.value());
		ExcelResult ret = new ExcelResult();
		try {
			ExcelRequest er = om.readValue(record.value(), ExcelRequest.class);
			List<ExcelCell> reply = engine.cellCalculation(er.getResource(), er.getSheet(), er.getOutputs(),
					er.getInputs(), false);

			ret.setUuid(er.getUuid());
			ret.setCount(reply.size());
			ret.setCells(reply);

			LOG.info("{}", om.writeValueAsString(reply));
		} catch (Exception ex) {
			LOG.error("Cannot execute {}", ex);
			
			ExcelError err = new ExcelError();

			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			ex.printStackTrace(pw);

			err.setCode(ex.getClass().getCanonicalName());
			err.setError(sw.toString());
			ret.setError(err);
		}
		return ret;

	}
}