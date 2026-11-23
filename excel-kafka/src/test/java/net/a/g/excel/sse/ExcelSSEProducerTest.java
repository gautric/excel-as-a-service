package net.a.g.excel.sse;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import net.a.g.excel.kafka.KafkaTestSupport;
import net.a.g.excel.model.ExcelCell;
import net.a.g.excel.model.ExcelError;
import net.a.g.excel.model.ExcelResult;

/** ExcelSSEProducer with fake Sse / SseEventSink: each built event is a map of the builder calls. */
public class ExcelSSEProducerTest {

	private ExcelSSEProducer sse;
	private ObjectMapper om;

	@BeforeEach
	public void setup() {
		om = KafkaTestSupport.om();
		sse = new ExcelSSEProducer();
		sse.om = om;
		sse.setSse(KafkaTestSupport.fake(Sse.class, (name, args) -> builder()));
	}

	private static OutboundSseEvent.Builder builder() {
		Map<String, Object> props = new HashMap<>();
		OutboundSseEvent.Builder[] self = new OutboundSseEvent.Builder[1];
		self[0] = KafkaTestSupport.fake(OutboundSseEvent.Builder.class, (name, args) -> {
			if ("build".equals(name)) {
				Map<String, Object> snapshot = new HashMap<>(props);
				return KafkaTestSupport.fake(OutboundSseEvent.class, (getter, a) -> snapshot.get(getter));
			}
			props.put("get" + Character.toUpperCase(name.charAt(0)) + name.substring(1), args[0]);
			return self[0];
		});
		return self[0];
	}

	private static SseEventSink sink(List<OutboundSseEvent> received) {
		return KafkaTestSupport.fake(SseEventSink.class, (name, args) -> {
			if ("send".equals(name)) {
				received.add((OutboundSseEvent) args[0]);
				return CompletableFuture.completedFuture(null);
			}
			return false; // isClosed
		});
	}

	private static ExcelResult ok(String uuid, double score) {
		ExcelCell c = new ExcelCell();
		c.setAddress("ComputeKYC!C6");
		c.setValue(score);
		ExcelResult r = new ExcelResult(List.of(c));
		r.setUuid(uuid);
		return r;
	}

	@Test
	public void resultWithoutSubscriberIsDropped() {
		ExcelSSEProducer noSse = new ExcelSSEProducer();
		noSse.om = om; // setSse never called and no sink: must not fail
		assertDoesNotThrow(() -> noSse.onMessage(ok("u", 1.0)));
	}

	@Test
	public void resultIsPushedAsNamedJsonEventWithUuidAsId() throws Exception {
		List<OutboundSseEvent> received = new ArrayList<>();
		sse.consume(sink(received));

		sse.onMessage(ok("sse-1", 125.0));

		assertEquals(1, received.size());
		OutboundSseEvent e = received.get(0);
		assertEquals("message", e.getName());
		assertEquals("sse-1", e.getId());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, e.getMediaType());
		assertEquals(3000L, e.getReconnectDelay());
		JsonNode data = om.readTree((String) e.getData());
		assertEquals("sse-1", data.get("uuid").asText());
		assertEquals(125.0, data.get("cells").get(0).get("value").asDouble());
	}

	@Test
	public void errorResultIsPushedWithItsCode() throws Exception {
		List<OutboundSseEvent> received = new ArrayList<>();
		sse.consume(sink(received));
		ExcelResult r = new ExcelResult();
		r.setUuid("sse-err");
		ExcelError err = new ExcelError();
		err.setCode("404");
		err.setError("Excel resource not found: NOPE");
		r.setError(err);

		sse.onMessage(r);

		JsonNode data = om.readTree((String) received.get(0).getData());
		assertEquals("404", data.get("error").get("code").asText());
		assertEquals("sse-err", received.get(0).getId());
	}

	@Test
	public void eachResultIsASeparateEvent() throws Exception {
		List<OutboundSseEvent> received = new ArrayList<>();
		sse.consume(sink(received));
		sse.onMessage(ok("a", 1.0));
		sse.onMessage(ok("b", 2.0));
		assertEquals(List.of("a", "b"), received.stream().map(OutboundSseEvent::getId).toList());
		assertTrue(((String) received.get(1).getData()).contains("\"b\""));
	}

	@Test
	public void everySubscriberReceivesTheResult() throws Exception {
		List<OutboundSseEvent> first = new ArrayList<>();
		List<OutboundSseEvent> second = new ArrayList<>();
		sse.consume(sink(first));
		sse.consume(sink(second));

		sse.onMessage(ok("both", 1.0));

		assertEquals(1, first.size());
		assertEquals(1, second.size());
	}
}
