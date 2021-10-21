package net.a.g.excel.kafka;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.smallrye.reactive.messaging.kafka.Record;
import net.a.g.excel.engine.ExcelEngine;
import net.a.g.excel.model.ExcelRequest;

@ApplicationScoped
public class ExcelConsumer {

	public final static Logger LOG = LoggerFactory.getLogger(ExcelConsumer.class);
	
	@Inject
	ExcelEngine engine;
//	
	
	@Inject
	ObjectMapper om;

    @Incoming("eaas-in")
    public void receive(Record<String, String> record) {
        LOG.info("Received {} - {}", record.key(), record.value());
        
        try {
			ExcelRequest er = om.readValue(record.value(), ExcelRequest.class);
	      //  engine.cellCalculation(er.getResource(), er.getSheet(), er.getOutputs(), er.getInputs(), false);

		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
    }
}