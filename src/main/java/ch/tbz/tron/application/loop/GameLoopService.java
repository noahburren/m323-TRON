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

@Service
@EnableScheduling
public class GameLoopService {

    private final TronWebSocketHandler handler;
    private final ObjectMapper om = new ObjectMapper();

    private volatile State state = initialState();

    // Pause / Restart-Logik
    private volatile boolean paused = false;
    private volatile long restartAtMillis = 0L;
    private volatile String winnerId = null;

    public GameLoopService(TronWebSocketHandler handler) {
        this.handler = handler;
    }

    // ~30 FPS (33ms). Wenn du langsamer willst: 50 (20 FPS) oder 100 (10 FPS)
    @Scheduled(fixedRate = 33)
    public void tick() {
        long now = System.currentTimeMillis();

        // 1) Pause-Phase: nichts berechnen, nur anzeigen, dann nach 1s reset
        if (paused) {
            if (now >= restartAtMillis) {
                state = initialState();
                paused = false;
            }
            broadcastState();
            return;
        }

        // 2) Events dieses ticks sammeln
        List<GameEvent> events = drainTurnEvents();

        // 3) tick (PURE)
        state = Engine.tick(state, events);

        // 4) Wenn fertig: dead players entfernen, 1s Pause starten
        if (state.status() == GameStatus.FINISHED) {

            // Gewinner bestimmen (wer lebt noch?)
            List<Player> alivePlayers = state.players().stream()
                    .filter(Player::alive)
                    .toList();

            if (alivePlayers.size() == 1) {
                winnerId = alivePlayers.get(0).id();
            } else if (alivePlayers.isEmpty()) {
                winnerId = "TIE";
            } else {
                winnerId = null;
            }

            state = removeDeadPlayers(state);

            paused = true;
            restartAtMillis = now + 1000;
        }

        // 5) broadcast (IMPURE)
        broadcastState();
    }

    private List<GameEvent> drainTurnEvents() {
        Collection<TurnEvent> turns = handler.latestTurns().values();
        return new ArrayList<>(turns);
    }

    private void broadcastState() {
        try {
            String payload = om.writeValueAsString(Map.of(
                    "type", "STATE",
                    "state", state,
                    "winner", winnerId
            ));
            TextMessage msg = new TextMessage(payload);

            for (WebSocketSession s : handler.sessions().values()) {
                if (s.isOpen()) {
                    s.sendMessage(msg);
                }
            }
        } catch (Exception ignored) {
            // minimal
        }
    }

    private static State removeDeadPlayers(State s) {

        // IDs der toten Spieler
        Set<String> deadIds = s.players().stream()
                .filter(p -> !p.alive())
                .map(Player::id)
                .collect(java.util.stream.Collectors.toSet());

        // Nur lebende Spieler behalten
        List<Player> alivePlayers = s.players().stream()
                .filter(Player::alive)
                .toList();

        // Nur Trails der lebenden Spieler behalten
        List<Trail> remainingWalls = s.walls().stream()
                .filter(t -> !deadIds.contains(t.ownerId()))
                .toList();

        return new State(
                s.tick(),
                s.width(),
                s.height(),
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

        return new State(
                0,
                width,
                height,
                List.of(p1, p2),
                List.of(), // falls du walls als List<Trail> hast. Wenn Set: Set.of()
                GameStatus.RUNNING
        );
    }
}