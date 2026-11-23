package net.a.g.excel.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.smallrye.reactive.messaging.kafka.Record;
import jakarta.ws.rs.core.Response;
import net.a.g.excel.model.ExcelRequest;
import net.a.g.excel.model.ExcelResult;

/** ExcelProducer and ExcelKafkaResource with a recording Emitter instead of a broker. */
public class ExcelProducerTest {

	private final List<Record<String, String>> sent = new ArrayList<>();
	private RuntimeException failure;
	private ExcelProducer producer;
	private ObjectMapper om;

	@SuppressWarnings("unchecked")
	@BeforeEach
	public void setup() {
		om = KafkaTestSupport.om();
		producer = new ExcelProducer();
		producer.om = om;
		producer.emitter = KafkaTestSupport.fake(Emitter.class, (name, args) -> {
			if (!"send".equals(name)) {
				return false; // isCancelled / hasRequests
			}
			if (failure != null) {
				throw failure;
			}
			sent.add((Record<String, String>) args[0]);
			return CompletableFuture.completedFuture(null);
		});
	}

	private static ExcelRequest kyc(String uuid) {
		ExcelRequest req = new ExcelRequest(uuid);
		req.setResource("KYCAPI");
		req.setSheet("ComputeKYC");
		req.setOutputs(List.of("SCORE"));
		req.setInputs(Map.of("PEP", "true", "COUNTRY", "CY", "AMOUNT", "1000000"));
		return req;
	}

	@Test
	public void sendsOneRecordKeyedByUuidWithRequestAsJson() throws Exception {
		producer.sendRequest(kyc("p-1"));

		assertEquals(1, sent.size());
		Record<String, String> r = sent.get(0);
		assertEquals("p-1", r.key());
		ExcelRequest back = om.readValue(r.value(), ExcelRequest.class);
		assertEquals("p-1", back.getUuid());
		assertEquals("KYCAPI", back.getResource());
		assertEquals("ComputeKYC", back.getSheet());
		assertEquals(List.of("SCORE"), back.getOutputs());
		assertEquals(Map.of("PEP", "true", "COUNTRY", "CY", "AMOUNT", "1000000"), back.getInputs());
	}

	@Test
	public void nullFieldsAreLeftOutOfThePayload() throws Exception {
		ExcelRequest req = new ExcelRequest("p-2");
		req.setResource("KYCAPI");
		producer.sendRequest(req);

		JsonNode n = om.readTree(sent.get(0).value());
		assertFalse(n.has("sheet"));
		assertFalse(n.has("global"));
		assertEquals("KYCAPI", n.get("resource").asText());
	}

	@Test
	public void defaultRequestGetsAGeneratedUuidAsKey() {
		producer.sendRequest(new ExcelRequest());
		producer.sendRequest(new ExcelRequest());
		assertEquals(2, sent.size());
		assertNotEquals(sent.get(0).key(), sent.get(1).key());
	}

	@Test
	public void producedRecordIsComputedByTheConsumer() {
		producer.sendRequest(kyc("round-trip"));

		ExcelConsumer consumer = new ExcelConsumer();
		consumer.engine = KafkaTestSupport.engine();
		consumer.om = om;
		ExcelResult res = consumer.receive(sent.get(0));

		assertNull(res.getError());
		assertEquals("round-trip", res.getUuid());
		assertEquals(125.0, res.getCells().iterator().next().getValue());
	}

	@Test
	public void resourceAnswers202AndForwardsTheRequest() {
		ExcelKafkaResource resource = new ExcelKafkaResource();
		resource.producer = producer;

		Response resp = resource.cellBody(kyc("rest-1"));

		assertEquals(202, resp.getStatus());
		assertFalse(resp.hasEntity());
		assertEquals(1, sent.size());
		assertEquals("rest-1", sent.get(0).key());
	}

	@Test
	public void sendFailureIsReported() {
		failure = new IllegalStateException("broker down");
		assertThrows(IllegalStateException.class, () -> producer.sendRequest(kyc("lost")));
		assertTrue(sent.isEmpty());
	}

	@Test
	public void resourceDoesNotAccept202WhenSendFails() {
		failure = new IllegalStateException("broker down");
		ExcelKafkaResource resource = new ExcelKafkaResource();
		resource.producer = producer;

		int status;
		try {
			status = resource.cellBody(kyc("lost")).getStatus();
		} catch (RuntimeException e) {
			status = 500;
		}
		assertNotEquals(202, status);
	}
}
