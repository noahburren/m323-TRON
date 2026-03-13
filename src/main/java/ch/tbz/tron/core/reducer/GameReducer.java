package ch.tbz.tron.core.reducer;

import ch.tbz.tron.core.model.*;
import ch.tbz.tron.events.GameEvent;
import ch.tbz.tron.events.TurnEvent;

import java.util.List;

public final class GameReducer {
    private GameReducer() {}

    /**
     * PURE: State + Event -> neuer State.
     * Keine I/O, keine Mutation des Input-States.
     */
    public static State reduce(State state, GameEvent event) {
        if (state.status() == GameStatus.FINISHED) {
            return state;
        }

        if (event instanceof TurnEvent turn) {
            return applyTurn(state, turn);
        }

        return state;
    }

    public static State reduceAll(State state, List<GameEvent> events) {
        State current = state;
        for (GameEvent e : events) {
            current = reduce(current, e);
        }
        return current;
    }

    private static State applyTurn(State state, TurnEvent turn) {
        List<Player> updated = state.players().stream()
                .map(p -> {
                    if (!p.id().equals(turn.playerId())) return p;
                    if (!p.alive()) return p;

                    // kein 180° Turn
                    if (p.direction().isOpposite(turn.direction())) return p;

                    return p.withDirection(turn.direction());
                })
                .toList();

        return new State(
                state.tick(),
                state.width(),
                state.height(),
                List.copyOf(updated),
                state.walls(), // walls bleibt unverändert
                state.status()
        );
    }
}