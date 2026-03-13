package ch.tbz.tron.core.model;

public record Position(int x, int y) {
    public Position move(Direction dir) {
        return new Position(x + dir.dx, y + dir.dy);
    }
}