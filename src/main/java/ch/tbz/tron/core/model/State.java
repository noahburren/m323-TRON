package ch.tbz.tron.core.model;

import java.util.List;

public record State(
        int tick,
        int width,
        int height,
        List<Player> players,
        List<Trail> walls,
        GameStatus status
) {
    public boolean inBounds(Position p) {
        return p.x() >= 0 && p.x() < width && p.y() >= 0 && p.y() < height;
    }
}