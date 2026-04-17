package ch.tbz.tron.core.logic;

import ch.tbz.tron.core.model.Direction;
import ch.tbz.tron.core.model.State;
import ch.tbz.tron.core.reducer.GameReducer;
import ch.tbz.tron.events.GameEvent;
import ch.tbz.tron.events.TurnEvent;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class Engine {
    private Engine() {}

    /**
     * PURE: complete tick.
     * Events are applied (pure), an input map is built (pure),
     * then StepLogic handles movement and collision (pure).
     */
    public static State tick(State state, List<GameEvent> eventsThisTick) {
        State reduced = GameReducer.reduceAll(state, eventsThisTick);
        Map<String, Direction> inputs = toInputs(eventsThisTick);
        return StepLogic.step(reduced, inputs);
    }

    /** Collapses all TurnEvents into a playerId→Direction map; last event per player wins. */
    private static Map<String, Direction> toInputs(List<GameEvent> events) {
        return events.stream()
                .filter(e -> e instanceof TurnEvent)
                .map(e -> (TurnEvent) e)
                .collect(Collectors.toMap(TurnEvent::playerId, TurnEvent::direction, (a, b) -> b));
    }
}
