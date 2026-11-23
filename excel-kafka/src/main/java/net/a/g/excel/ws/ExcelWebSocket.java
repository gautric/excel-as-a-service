package net.a.g.excel.ws;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import net.a.g.excel.model.ExcelRequest;
import net.a.g.excel.model.ExcelResult;
import net.a.g.excel.util.ExcelErrors;

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
	public void onWebSocketMessage(Session session, String message) throws JsonProcessingException {
		LOG.info("onWebSocketMessage {}", message);

		ExcelResult ret;
		ExcelRequest er = null;
		try {
			er = om.readValue(message, ExcelRequest.class);
			ret = engine.compute(er);
		} catch (JsonProcessingException ex) {
			ret = ExcelErrors.toResult(new IllegalArgumentException("Invalid JSON request: " + ex.getOriginalMessage()),
					null);
		} catch (Exception ex) {
			ret = ExcelErrors.toResult(ex, er == null ? null : er.getUuid());
		}

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