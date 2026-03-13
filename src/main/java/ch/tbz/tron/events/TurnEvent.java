package ch.tbz.tron.events;

import ch.tbz.tron.core.model.Direction;

public record TurnEvent(String playerId, Direction direction) implements GameEvent {
}