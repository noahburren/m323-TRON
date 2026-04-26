package ch.tbz.tron.core.reducer;

import ch.tbz.tron.core.model.*;
import ch.tbz.tron.events.GameEvent;
import ch.tbz.tron.events.TurnEvent;

import java.util.List;

public final class GameReducer {
    private GameReducer() {}

    /**
     * PURE: (State, Event) → new State.
     * No I/O, no mutation of the input state.
     */
    public static State reduce(State state, GameEvent event) {
        if (state.status() == GameStatus.FINISHED) return state;

        if (event instanceof TurnEvent turn) {
            return applyTurn(state, turn);
        }

        return state;
    }

    /**
     * PURE recursive fold: applies each event in order.
     * Base case: empty list returns state unchanged.
     */
    public static State reduceAll(State state, List<GameEvent> events) {
        if (events.isEmpty()) return state;
        return reduceAll(reduce(state, events.get(0)), events.subList(1, events.size()));
    }

    private static State applyTurn(State state, TurnEvent turn) {
        List<Player> updated = state.players().stream()
                .map(p -> {
                    if (!p.id().equals(turn.playerId())) return p;
                    if (!p.alive()) return p;
                    if (p.direction().isOpposite(turn.direction())) return p;
                    return p.withDirection(turn.direction());
                })
                .toList();

        return new State(
                state.tick(),
                state.width(),
                state.height(),
                List.copyOf(updated),
                state.walls(),
                state.status()
        );
    }
}
