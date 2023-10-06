package net.a.g.excel.ws;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import net.a.g.excel.engine.ExcelEngine;
import net.a.g.excel.model.ExcelCell;
import net.a.g.excel.model.ExcelRequest;
import net.a.g.excel.model.ExcelResult;

@ServerEndpoint("/eaas/ws")
@ApplicationScoped
public class ExcelWebSocket {

	private final Logger LOG = LoggerFactory.getLogger(ExcelWebSocket.class);

	Map<String, Session> sessions = new ConcurrentHashMap<>();

	@Inject
	ExcelEngine engine;

	@Inject
	ObjectMapper om;

	@OnOpen
	public void onOpen(Session session) {
		LOG.info("onOpen {}", session.getId());
		sessions.put(session.getId(), session);
	}

	@OnClose
	public void onClose(Session session) {
		LOG.info("onClose {}", session.getId());
		sessions.remove(session.getId());
	}

	@OnError
	public void onError(Session session, Throwable throwable) {
		LOG.error("onError {} {}", session.getId(), throwable);
		sessions.remove(session.getId());
	}

	@OnMessage
	public void onWebSocketMessage(Session session, String message)
			throws JsonMappingException, JsonProcessingException {
		LOG.info("onWebSocketMessage {}", message);

		ExcelRequest er = om.readValue(message, ExcelRequest.class);

		ExcelResult ret = new ExcelResult();

		List<ExcelCell> reply = engine.cellCalculation(er.getResource(), er.getSheet(), er.getOutputs(), er.getInputs(),
				false);

		ret.setUuid(er.getUuid());
		ret.setCount(reply.size());
		ret.setCells(reply);

		String result = om.writeValueAsString(ret);
		LOG.info("{}", result);

		session.getAsyncRemote().sendObject(result, callback -> {
			if (callback.getException() != null) {
				LOG.error("Unable to push message", callback.getException());
			}
		});
	}

	@Incoming("eaas-response-channel")
	public void onChannelMessage(ExcelResult result) throws IOException {

		String payload = om.writeValueAsString(result);
		LOG.info("Received Result : {} ", payload);
		broadcast(payload);
	}

	private void broadcast(String message) {
		sessions.values().forEach(s -> {
			s.getAsyncRemote().sendObject(message, callback -> {
				if (callback.getException() != null) {
					LOG.error("Unable to push message", callback.getException());
				}
			});
		});
	}
}