package net.a.g.excel.ws;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.a.g.excel.model.ExcelResult;

@ServerEndpoint("/eaas/ws")
@ApplicationScoped
public class ExcelWebSocket {

	private final Logger LOG = LoggerFactory.getLogger(ExcelWebSocket.class);

	Map<String, Session> sessions = new ConcurrentHashMap<>();

	@Inject
	ObjectMapper om;

	@OnOpen
	public void onOpen(Session session) {
		sessions.put(session.getId(), session);
	}

	@OnClose
	public void onClose(Session session) {
		sessions.remove(session.getId());
	}

	@OnError
	public void onError(Session session, Throwable throwable) {
		sessions.remove(session.getId());
	}

	@OnMessage
	public void onMessage(Session session, String message) {
//		broadcast(">> " + session.getId() + ": " + message);

	}

	@Incoming("eaas-response-channel")
	public void onMessage(ExcelResult result) throws IOException {

		String payload = om.writeValueAsString(result);

		LOG.info("Received Result : {} ", payload);

		broadcast(payload);
	}

	private void broadcast(String message) {
		sessions.values().forEach(s -> {
			s.getAsyncRemote().sendObject(message, result -> {
				if (result.getException() != null) {
					LOG.error("Unable to push message", result.getException());
				}
			});
		});
	}

}