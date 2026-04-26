package ch.tbz.tron.core.logic;

import ch.tbz.tron.core.model.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class StepLogic {
    private StepLogic() {}

    /**
     * PURE: no I/O, no timer, no network, no mutation of the input state.
     * inputs: desired direction per player for this tick (may be empty).
     */
    public static State step(State state, Map<String, Direction> inputs) {
        if (state.status() == GameStatus.FINISHED) return state;

        // 1) Apply direction inputs — 180° reversal is blocked
        List<Player> directed = state.players().stream()
                .map(p -> applyDirection(p, inputs.get(p.id())))
                .toList();

        // 2) Append alive players' current positions to the trail wall (order preserved)
        List<Trail> newWalls = Stream.concat(
                state.walls().stream(),
                directed.stream()
                        .filter(Player::alive)
                        .map(p -> new Trail(p.position(), p.id()))
        ).collect(Collectors.toList());

        // 3) Build occupied set for O(1) collision lookup
        Set<Position> occupied = newWalls.stream()
                .map(Trail::position)
                .collect(Collectors.toSet());

        // 4) Compute move proposals: current position (from) and next position (to)
        Map<String, Position> from = directed.stream()
                .collect(Collectors.toMap(Player::id, Player::position));
        Map<String, Position> to = directed.stream()
                .filter(Player::alive)
                .collect(Collectors.toMap(Player::id, p -> p.position().move(p.direction())));

        // 5) Determine dead players
        Set<String> dead = new HashSet<>();

        // 5.1 Out-of-bounds or wall collision
        directed.stream()
                .filter(Player::alive)
                .filter(p -> !state.inBounds(to.get(p.id())) || occupied.contains(to.get(p.id())))
                .map(Player::id)
                .forEach(dead::add);

        // 5.2 Head-on: multiple players targeting the same cell
        to.entrySet().stream()
                .collect(Collectors.groupingBy(Map.Entry::getValue,
                         Collectors.mapping(Map.Entry::getKey, Collectors.toList())))
                .values().stream()
                .filter(ids -> ids.size() >= 2)
                .flatMap(List::stream)
                .forEach(dead::add);

        // 5.3 Swap collision: A moves to B's origin and B moves to A's origin
        List<String> ids = List.copyOf(to.keySet());
        for (int i = 0; i < ids.size(); i++) {
            for (int j = i + 1; j < ids.size(); j++) {
                String a = ids.get(i), b = ids.get(j);
                if (Objects.equals(to.get(a), from.get(b)) && Objects.equals(to.get(b), from.get(a))) {
                    dead.add(a);
                    dead.add(b);
                }
            }
        }

        // 6) Move alive, non-dead players to their next position
        List<Player> moved = directed.stream()
                .map(p -> {
                    if (!p.alive()) return p;
                    if (dead.contains(p.id())) return p.withAlive(false);
                    return p.withPosition(to.get(p.id()));
                })
                .toList();

        // 7) Determine new game status
        GameStatus newStatus = moved.stream().filter(Player::alive).count() <= 1
                ? GameStatus.FINISHED
                : GameStatus.RUNNING;

        return new State(
                state.tick() + 1,
                state.width(),
                state.height(),
                List.copyOf(moved),
                List.copyOf(newWalls),
                newStatus
        );
    }

    private static Player applyDirection(Player p, Direction desired) {
        if (!p.alive()) return p;
        if (desired == null) return p;
        if (p.direction().isOpposite(desired)) return p;
        return p.withDirection(desired);
    }
}
