package ch.tbz.tron.core.logic;

import ch.tbz.tron.core.model.*;

import java.util.*;

public final class StepLogic {
    private StepLogic() {}

    /**
     * PURE: Keine I/O, kein Timer, kein Netzwerk, keine Mutation am Input-State.
     * inputs: letzte gewünschte Richtung pro Spieler (optional).
     */
    public static State step(State state, Map<String, Direction> inputs) {
        if (state.status() == GameStatus.FINISHED) {
            return state;
        }

        // 1) Richtung anwenden (kein 180° Turn)
        List<Player> directed = state.players().stream()
                .map(p -> applyDirection(p, inputs.get(p.id())))
                .toList();

        // 2) Trail: aktuelle Positionen lebender Spieler werden an die Walls angehängt (ORDERED!)
        List<Trail> newWalls = new ArrayList<>(state.walls());
        for (Player p : directed) {
            if (p.alive()) {
                newWalls.add(new Trail(p.position(), p.id()));
            }
        }

        // 2.1 Occupied-Set für schnelle Collision (Positionen aller Trails)
        Set<Position> occupied = new HashSet<>();
        for (Trail t : newWalls) {
            occupied.add(t.position());
        }

        // 3) Move proposals berechnen (from -> to)
        Map<String, Position> from = new HashMap<>();
        Map<String, Position> to = new HashMap<>();
        for (Player p : directed) {
            from.put(p.id(), p.position());
            if (p.alive()) {
                to.put(p.id(), p.position().move(p.direction()));
            }
        }

        // 4) Kollisionen bestimmen
        Set<String> dead = new HashSet<>();

        // 4.1 Out of bounds oder in Wall
        for (Player p : directed) {
            if (!p.alive()) continue;
            Position next = to.get(p.id());

            if (!state.inBounds(next)) {
                dead.add(p.id());
                continue;
            }

            if (occupied.contains(next)) {
                dead.add(p.id());
            }
        }

        // 4.2 Head-on: mehrere Spieler in dieselbe Zelle
        Map<Position, List<String>> byTarget = new HashMap<>();
        for (var e : to.entrySet()) {
            byTarget.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
        }
        for (var entry : byTarget.entrySet()) {
            if (entry.getValue().size() >= 2) {
                dead.addAll(entry.getValue());
            }
        }

        // 4.3 Swap-Collision: A geht nach B.from und B nach A.from
        List<String> ids = new ArrayList<>(to.keySet());
        for (int i = 0; i < ids.size(); i++) {
            for (int j = i + 1; j < ids.size(); j++) {
                String a = ids.get(i);
                String b = ids.get(j);
                if (Objects.equals(to.get(a), from.get(b)) && Objects.equals(to.get(b), from.get(a))) {
                    dead.add(a);
                    dead.add(b);
                }
            }
        }

        // 5) Spieler aktualisieren (Position nur wenn nicht dead)
        List<Player> moved = directed.stream()
                .map(p -> {
                    if (!p.alive()) return p;
                    if (dead.contains(p.id())) return p.withAlive(false);
                    return p.withPosition(to.get(p.id()));
                })
                .toList();

        // 6) Status setzen
        long aliveCount = moved.stream().filter(Player::alive).count();
        GameStatus newStatus = (aliveCount <= 1) ? GameStatus.FINISHED : GameStatus.RUNNING;

        return new State(
                state.tick() + 1,
                state.width(),
                state.height(),
                List.copyOf(moved),
                List.copyOf(newWalls),   // WICHTIG: List, nicht Set
                newStatus
        );
    }

    private static Player applyDirection(Player p, Direction desired) {
        if (!p.alive()) return p;
        if (desired == null) return p;
        if (p.direction().isOpposite(desired)) return p; // kein 180° Turn
        return p.withDirection(desired);
    }
}