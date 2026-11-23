package net.a.g.excel.ws;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.SendHandler;
import jakarta.websocket.SendResult;
import jakarta.websocket.Session;
import net.a.g.excel.kafka.KafkaTestSupport;
import net.a.g.excel.model.ExcelResult;

/** ExcelWebSocket with fake Sessions recording what is pushed to them. */
public class ExcelWebSocketTest {

	private ExcelWebSocket ws;
	private ObjectMapper om;

	/** A fake session; {@code failSend} makes the async send report an exception to its handler. */
	private static final class FakeSession {
		final List<String> pushed = new ArrayList<>();
		final Session session;

		FakeSession(String id, boolean failSend) {
			RemoteEndpoint.Async remote = KafkaTestSupport.fake(RemoteEndpoint.Async.class, (name, args) -> {
				if ("sendObject".equals(name)) {
					pushed.add((String) args[0]);
					((SendHandler) args[1])
							.onResult(failSend ? new SendResult(new java.io.IOException("gone")) : new SendResult());
				}
				return null;
			});
			session = KafkaTestSupport.fake(Session.class, (name, args) -> switch (name) {
			case "getId" -> id;
			case "getAsyncRemote" -> remote;
			default -> null;
			});
		}
	}

	@BeforeEach
	public void setup() {
		om = KafkaTestSupport.om();
		ws = new ExcelWebSocket();
		ws.engine = KafkaTestSupport.engine();
		ws.om = om;
	}

	private static ExcelResult result(String uuid) {
		ExcelResult r = new ExcelResult();
		r.setUuid(uuid);
		return r;
	}

	@Test
	public void openCloseAndErrorTrackSessions() {
		FakeSession a = new FakeSession("a", false);
		FakeSession b = new FakeSession("b", false);
		ws.onOpen(a.session);
		ws.onOpen(b.session);
		assertEquals(2, ws.sessions.size());

		ws.onClose(a.session);
		assertEquals(List.of("b"), List.copyOf(ws.sessions.keySet()));

		ws.onError(b.session, new RuntimeException("reset"));
		assertTrue(ws.sessions.isEmpty());
	}

	@Test
	public void channelResultIsBroadcastToEveryOpenSession() throws Exception {
		FakeSession a = new FakeSession("a", false);
		FakeSession b = new FakeSession("b", false);
		ws.onOpen(a.session);
		ws.onOpen(b.session);

		ws.onChannelMessage(result("bc-1"));

		assertEquals(1, a.pushed.size());
		assertEquals(a.pushed, b.pushed);
		assertEquals("bc-1", om.readTree(a.pushed.get(0)).get("uuid").asText());
	}

	@Test
	public void closedSessionNoLongerReceivesBroadcasts() throws Exception {
		FakeSession a = new FakeSession("a", false);
		ws.onOpen(a.session);
		ws.onClose(a.session);

		ws.onChannelMessage(result("late"));

		assertTrue(a.pushed.isEmpty());
	}

	@Test
	public void failedPushToOneSessionDoesNotStopTheOthers() throws Exception {
		FakeSession broken = new FakeSession("broken", true);
		FakeSession fine = new FakeSession("fine", false);
		ws.onOpen(broken.session);
		ws.onOpen(fine.session);

		assertDoesNotThrow(() -> ws.onChannelMessage(result("x")));
		assertEquals(1, fine.pushed.size());
	}

	@Test
	public void cellReferenceRequestIsComputedAndRepliedToTheSenderOnly() throws Exception {
		FakeSession sender = new FakeSession("sender", false);
		FakeSession other = new FakeSession("other", false);
		ws.onOpen(sender.session);
		ws.onOpen(other.session);

		ws.onWebSocketMessage(sender.session, "{\"uuid\":\"ws-1\",\"resource\":\"KYC\",\"sheet\":\"ComputeKYC\","
				+ "\"outputs\":[\"C6\",\"C2\"],\"inputs\":{\"B2\":\"true\",\"B3\":\"CY\",\"B4\":\"1000000\"}}");

		assertTrue(other.pushed.isEmpty());
		JsonNode n = om.readTree(sender.pushed.get(0));
		assertEquals("ws-1", n.get("uuid").asText());
		assertEquals(2, n.get("_count").asInt());
		assertEquals(125.0, n.get("cells").get(0).get("value").asDouble());
		assertEquals(50.0, n.get("cells").get(1).get("value").asDouble());
	}

	@Test
	public void invalidJsonIsRepliedAsBadRequest() throws Exception {
		FakeSession s = new FakeSession("s", false);
		ws.onWebSocketMessage(s.session, "{broken");
		assertEquals("400", om.readTree(s.pushed.get(0)).get("error").get("code").asText());
	}

	@Test
	public void apiNamesWorkLikeOnKafka() throws Exception {
		FakeSession s = new FakeSession("s", false);
		ws.onWebSocketMessage(s.session, "{\"uuid\":\"ws-2\",\"resource\":\"KYCAPI\",\"sheet\":\"ComputeKYC\","
				+ "\"outputs\":[\"SCORE\"],\"inputs\":{\"PEP\":\"true\",\"COUNTRY\":\"CY\",\"AMOUNT\":\"1000000\"}}");
		assertEquals(125.0, om.readTree(s.pushed.get(0)).get("cells").get(0).get("value").asDouble());
	}

	@Test
	public void engineErrorIsRepliedAsErrorResultWithUuid() throws Exception {
		FakeSession s = new FakeSession("s", false);
		ws.onWebSocketMessage(s.session,
				"{\"uuid\":\"ws-3\",\"resource\":\"NOPE\",\"sheet\":\"ComputeKYC\",\"outputs\":[\"C6\"]}");
		JsonNode n = om.readTree(s.pushed.get(0));
		assertEquals("ws-3", n.get("uuid").asText());
		assertEquals("404", n.get("error").get("code").asText());
	}
}
