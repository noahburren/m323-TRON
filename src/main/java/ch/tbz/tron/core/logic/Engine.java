package ch.tbz.tron.core.logic;

import ch.tbz.tron.core.model.Direction;
import ch.tbz.tron.core.model.State;
import ch.tbz.tron.core.reducer.GameReducer;
import ch.tbz.tron.events.GameEvent;
import ch.tbz.tron.events.TurnEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Engine {
    private Engine() {}

    /**
     * PURE: kompletter Tick.
     * - Events werden angewendet (pure)
     * - daraus wird eine Input-Map gebaut (pure)
     * - StepLogic macht Bewegung/Kollision (pure)
     */
    public static State tick(State state, List<GameEvent> eventsThisTick) {
        // 1) Richtung im State aktualisieren (Reducer)
        State reduced = GameReducer.reduceAll(state, eventsThisTick);

        // 2) zusätzlich eine Input-Map aufbauen (letztes TURN-Event gewinnt)
        Map<String, Direction> inputs = toInputs(eventsThisTick);

        // 3) Bewegung + Collision
        return StepLogic.step(reduced, inputs);
    }

    private static Map<String, Direction> toInputs(List<GameEvent> events) {
        Map<String, Direction> inputs = new HashMap<>();
        for (GameEvent e : events) {
            if (e instanceof TurnEvent t) {
                inputs.put(t.playerId(), t.direction());
            }
        }
        return inputs;
    }
}