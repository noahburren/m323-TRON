package ch.tbz.tron.core.model;

public record Player(
        String id,
        Position position,
        Direction direction,
        boolean alive
) {
    public Player withDirection(Direction newDirection) {
        return new Player(id, position, newDirection, alive);
    }

    public Player withPosition(Position newPosition) {
        return new Player(id, newPosition, direction, alive);
    }

    public Player withAlive(boolean newAlive) {
        return new Player(id, position, direction, newAlive);
    }
}