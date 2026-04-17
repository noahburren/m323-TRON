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

    private final ObjectMapper objectMapper = new ObjectMapper();

    // sessionId → playerId
    private final Map<String, String> players = new ConcurrentHashMap<>();

    // playerId → last turn event (read once per tick)
    private final Map<String, TurnEvent> latestTurns = new ConcurrentHashMap<>();

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    private final AtomicInteger joinCount = new AtomicInteger(0);

    public Map<String, String> players() { return players; }
    public Map<String, TurnEvent> latestTurns() { return latestTurns; }
    public Map<String, WebSocketSession> sessions() { return sessions; }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);

        int playerNumber = joinCount.incrementAndGet();
        String playerId = (playerNumber == 1) ? "p1" : "p2";
        players.put(session.getId(), playerId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String playerId = players.remove(session.getId());
        if (playerId != null) {
            latestTurns.remove(playerId);
        }
        sessions.remove(session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode root = objectMapper.readTree(message.getPayload());

        // Expected formats:
        // Multiplayer:  {"type":"TURN","dir":"UP"}
        // Single-PC:    {"type":"TURN","playerId":"p1","dir":"UP"}
        String type = root.path("type").asText("");
        if (!"TURN".equals(type)) return;

        String dirStr = root.path("dir").asText("");
        if (dirStr.isBlank()) return;

        Direction dir;
        try {
            dir = Direction.valueOf(dirStr);
        } catch (IllegalArgumentException ex) {
            return;
        }

        String requestedPlayerId = root.path("playerId").asText(null);
        String playerId = (requestedPlayerId != null && !requestedPlayerId.isBlank())
                ? requestedPlayerId
                : players.get(session.getId());

        if (playerId == null) return;

        latestTurns.put(playerId, new TurnEvent(playerId, dir));
    }
}
