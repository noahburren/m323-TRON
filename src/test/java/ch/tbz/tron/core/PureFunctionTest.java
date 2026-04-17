package ch.tbz.tron.core;

import ch.tbz.tron.core.logic.StepLogic;
import ch.tbz.tron.core.model.*;
import ch.tbz.tron.core.reducer.GameReducer;
import ch.tbz.tron.events.GameEvent;
import ch.tbz.tron.events.TurnEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PureFunctionTest {

    // ── helpers ──────────────────────────────────────────────────────────────

    private static State stateWith(Player... players) {
        return new State(0, 10, 10, List.of(players), List.of(), GameStatus.RUNNING);
    }

    // ── GameReducer ───────────────────────────────────────────────────────────

    @Test
    void reducer_appliesTurnEvent() {
        // A TURN event must update the player's direction in the new state.
        Player p1 = new Player("p1", new Position(5, 5), Direction.UP, true);
        State result = GameReducer.reduce(stateWith(p1), new TurnEvent("p1", Direction.RIGHT));

        assertEquals(Direction.RIGHT, result.players().get(0).direction());
    }

    @Test
    void reducer_blocks180DegreeTurn() {
        // A player moving UP must not be allowed to reverse to DOWN.
        Player p1 = new Player("p1", new Position(5, 5), Direction.UP, true);
        State result = GameReducer.reduce(stateWith(p1), new TurnEvent("p1", Direction.DOWN));

        assertEquals(Direction.UP, result.players().get(0).direction());
    }

    @Test
    void reducer_reduceAll_appliesEventsRecursively() {
        // reduceAll is a recursive fold — every event must be applied in order.
        Player p1 = new Player("p1", new Position(2, 2), Direction.UP, true);
        Player p2 = new Player("p2", new Position(7, 7), Direction.DOWN, true);

        List<GameEvent> events = List.<GameEvent>of(
                new TurnEvent("p1", Direction.RIGHT),
                new TurnEvent("p2", Direction.LEFT)
        );
        State result = GameReducer.reduceAll(stateWith(p1, p2), events);

        assertEquals(Direction.RIGHT, result.players().get(0).direction());
        assertEquals(Direction.LEFT,  result.players().get(1).direction());
    }

    // ── StepLogic ─────────────────────────────────────────────────────────────

    @Test
    void step_playerAdvancesOneCellPerTick() {
        // A player moving RIGHT from (5,5) must end up at (6,5) after one step.
        Player p1 = new Player("p1", new Position(5, 5), Direction.RIGHT, true);
        State result = StepLogic.step(stateWith(p1), Map.of());

        assertEquals(new Position(6, 5), result.players().get(0).position());
        assertTrue(result.players().get(0).alive());
    }

    @Test
    void step_outOfBoundsKillsPlayer() {
        // A player at the right edge (x=9) moving RIGHT steps outside the 10x10 grid.
        Player p1 = new Player("p1", new Position(9, 5), Direction.RIGHT, true);
        State result = StepLogic.step(stateWith(p1), Map.of());

        assertFalse(result.players().get(0).alive());
        assertEquals(GameStatus.FINISHED, result.status());
    }

    @Test
    void step_wallCollisionKillsPlayer() {
        // A trail tile blocks the cell the player wants to enter.
        Player p1 = new Player("p1", new Position(5, 5), Direction.RIGHT, true);
        Trail blockingWall = new Trail(new Position(6, 5), "p2");
        State state = new State(0, 10, 10, List.of(p1), List.of(blockingWall), GameStatus.RUNNING);

        State result = StepLogic.step(state, Map.of());

        assertFalse(result.players().get(0).alive());
    }

    @Test
    void step_headOnCollisionKillsBothPlayers() {
        // p1 at (4,5) → RIGHT and p2 at (6,5) → LEFT both target (5,5): mutual kill.
        Player p1 = new Player("p1", new Position(4, 5), Direction.RIGHT, true);
        Player p2 = new Player("p2", new Position(6, 5), Direction.LEFT,  true);
        State result = StepLogic.step(stateWith(p1, p2), Map.of());

        assertFalse(result.players().get(0).alive(), "p1 should be dead");
        assertFalse(result.players().get(1).alive(), "p2 should be dead");
        assertEquals(GameStatus.FINISHED, result.status());
    }
}
