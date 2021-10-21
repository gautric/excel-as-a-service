package net.a.g.excel.ws;

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

import net.a.g.excel.engine.ExcelEngine;
import net.a.g.excel.util.ExcelConfiguration;

@ServerEndpoint("/ws")
@ApplicationScoped
public class ExcelWebSocket {

	Map<String, Session> sessions = new ConcurrentHashMap<>();

//	@Inject
//	ExcelConfiguration conf;
//
//	@Inject
//	ExcelEngine engine;

	@OnOpen
	public void onOpen(Session session) {
		sessions.put(session.getId(), session);
		broadcast("User " + session.getId() + " joined");
	}

	@OnClose
	public void onClose(Session session) {
		sessions.remove(session.getId());
		broadcast("User\" +session.getId()  + \" left");
	}

	@OnError
	public void onError(Session session, Throwable throwable) {
		sessions.remove(session.getId());

		broadcast("User " +session.getId()  + "left on error: " + throwable);
	}

	@OnMessage
	public void onMessage(Session session, String message) {
		broadcast(">> " + session.getId() + ": " + message);

//		try {
//			session.getAsyncRemote().sendObject("{ttt:true}", result -> {
//				if (result.getException() != null) {
//					System.out.println("Unable to send message: " + result.getException());
//				}
//			});
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}

	private void broadcast(String message) {
		sessions.values().forEach(s -> {
			s.getAsyncRemote().sendObject(message, result -> {
				if (result.getException() != null) {
					System.out.println("Unable to send message: " + result.getException());
				}
			});
		});
	}

}