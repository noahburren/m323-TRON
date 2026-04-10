package ch.tbz.tron.application.websocket;

import ch.tbz.tron.core.model.Direction;
import ch.tbz.tron.events.TurnEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class TronWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper om = new ObjectMapper();

    // sessionId -> playerId
    private final Map<String, String> players = new ConcurrentHashMap<>();

    // playerId -> last turn event (pro tick wird das ausgelesen)
    private final Map<String, TurnEvent> latestTurns = new ConcurrentHashMap<>();

    private final AtomicInteger joinCount = new AtomicInteger(0);

    public Map<String, String> players() {
        return players;
    }

    public Map<String, TurnEvent> latestTurns() {
        return latestTurns;
    }


    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);

        int n = joinCount.incrementAndGet();
        String playerId = (n == 1) ? "p1" : "p2"; // MVP 2 Spieler
        players.put(session.getId(), playerId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String playerId = players.remove(session.getId());
        if (playerId != null) {
            latestTurns.remove(playerId);
        }
        players.remove(session.getId());
        sessions.remove(session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode root = om.readTree(message.getPayload());

        // Erwartetes Format:
        // Multiplayer: {"type":"TURN","dir":"UP"}
        // 1-PC Mode:   {"type":"TURN","playerId":"p1","dir":"UP"}
        String type = root.path("type").asText("");
        if (!"TURN".equals(type)) return;

        String dirStr = root.path("dir").asText("");
        if (dirStr.isBlank()) return;

        Direction dir;
        try {
            dir = Direction.valueOf(dirStr);
        } catch (IllegalArgumentException ex) {
            return; // ungültige Direction ignorieren
        }

        // optional: client darf playerId mitgeben (für 1 PC Steuerung)
        String requestedPlayerId = root.path("playerId").asText(null);

        String playerId;
        if (requestedPlayerId != null && !requestedPlayerId.isBlank()) {
            playerId = requestedPlayerId;              // z.B. "p1" oder "p2"
        } else {
            playerId = players.get(session.getId());   // fallback
        }

        if (playerId == null) return;

        latestTurns.put(playerId, new TurnEvent(playerId, dir));
    }

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public Map<String, WebSocketSession> sessions() {
        return sessions;
    }
}