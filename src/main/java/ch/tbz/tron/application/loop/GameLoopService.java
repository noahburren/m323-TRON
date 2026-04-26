package ch.tbz.tron.application.loop;

import ch.tbz.tron.application.websocket.TronWebSocketHandler;
import ch.tbz.tron.core.logic.Engine;
import ch.tbz.tron.core.model.*;
import ch.tbz.tron.events.GameEvent;
import ch.tbz.tron.events.TurnEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.*;
import java.util.stream.Collectors;

@Service
@EnableScheduling
public class GameLoopService {

    private final TronWebSocketHandler handler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private volatile State state = initialState();

    // Pause / restart state — lives here as the intentional impure boundary
    private volatile boolean paused = false;
    private volatile long restartAtMillis = 0L;
    private volatile String winnerId = null;

    public GameLoopService(TronWebSocketHandler handler) {
        this.handler = handler;
    }

    // ~30 FPS (33 ms per tick)
    @Scheduled(fixedRate = 33)
    public void tick() {
        long now = System.currentTimeMillis();

        // Pause phase: hold the finished frame for 1 s, then reset
        if (paused) {
            if (now >= restartAtMillis) {
                state = initialState();
                paused = false;
            }
            broadcastState();
            return;
        }

        // Collect this tick's input events and advance the pure game state
        List<GameEvent> events = drainTurnEvents();
        state = Engine.tick(state, events);

        if (state.status() == GameStatus.FINISHED) {
            List<Player> alivePlayers = state.players().stream()
                    .filter(Player::alive)
                    .toList();

            winnerId = alivePlayers.size() == 1 ? alivePlayers.get(0).id()
                     : alivePlayers.isEmpty()   ? "TIE"
                     : null;

            state = removeDeadPlayers(state);
            paused = true;
            restartAtMillis = now + 1000;
        }

        broadcastState();
    }

    private List<GameEvent> drainTurnEvents() {
        Collection<TurnEvent> turns = handler.latestTurns().values();
        return new ArrayList<>(turns);
    }

    private void broadcastState() {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "type", "STATE",
                    "state", state,
                    "winner", winnerId
            ));
            TextMessage msg = new TextMessage(payload);

            for (WebSocketSession session : handler.sessions().values()) {
                if (session.isOpen()) {
                    session.sendMessage(msg);
                }
            }
        } catch (Exception ignored) {
            // minimal error handling — broadcast failures are non-fatal
        }
    }

    private static State removeDeadPlayers(State state) {
        Set<String> deadIds = state.players().stream()
                .filter(p -> !p.alive())
                .map(Player::id)
                .collect(Collectors.toSet());

        List<Player> alivePlayers = state.players().stream()
                .filter(Player::alive)
                .toList();

        List<Trail> remainingWalls = state.walls().stream()
                .filter(t -> !deadIds.contains(t.ownerId()))
                .toList();

        return new State(
                state.tick(),
                state.width(),
                state.height(),
                List.copyOf(alivePlayers),
                List.copyOf(remainingWalls),
                GameStatus.FINISHED
        );
    }

    private static State initialState() {
        int width = 140;
        int height = 80;
        int midY = height / 2;

        Player p1 = new Player("p1", new Position(2, midY), Direction.RIGHT, true);
        Player p2 = new Player("p2", new Position(width - 3, midY), Direction.LEFT, true);

        return new State(0, width, height, List.of(p1, p2), List.of(), GameStatus.RUNNING);
    }
}
