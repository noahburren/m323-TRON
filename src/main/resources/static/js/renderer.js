import "./types.js";
import { canvas, ctx } from "./ui.js";

/** Maps direction strings to unit vectors — avoids mutable let dx/dy reassignment */
const DIRECTION_VECTORS = {
    UP:    { dx: 0,  dy: -1 },
    DOWN:  { dx: 0,  dy:  1 },
    LEFT:  { dx: -1, dy:  0 },
    RIGHT: { dx: 1,  dy:  0 },
};

/**
 * @param {GameState} state
 * @param {string|null} winner
 */
export function render(state, winner) {
    const w = state.width, h = state.height;
    if (!w || !h) return;

    const rect = canvas.getBoundingClientRect();
    const viewW = rect.width;
    const viewH = rect.height;

    const cell = Math.floor(Math.min(viewW / w, viewH / h));
    const boardW = w * cell;
    const boardH = h * cell;
    const offX = Math.floor((viewW - boardW) / 2);
    const offY = Math.floor((viewH - boardH) / 2);

    ctx.clearRect(0, 0, viewW, viewH);

    ctx.fillStyle = "rgba(0,0,0,0.22)";
    ctx.fillRect(offX - 18, offY - 18, boardW + 36, boardH + 36);

    drawGrid(offX, offY, w, h, cell);

    glowRect(offX, offY, boardW, boardH, "rgba(0,240,255,0.18)", 18);
    ctx.strokeStyle = "rgba(0,240,255,0.42)";
    ctx.lineWidth = 1;
    ctx.strokeRect(offX + 0.5, offY + 0.5, boardW - 1, boardH - 1);

    drawWalls(state.walls || [], offX, offY, cell);
    drawPlayers(state.players || [], offX, offY, cell);

    if (state.status === "FINISHED") {
        const text = winner === "p1" ? "BLUE WINS"
                   : winner === "p2" ? "PINK WINS"
                   : winner === "TIE" ? "TIE"
                   : "GAME OVER";
        overlayText(offX, offY, boardW, boardH, text);
    }
}

/** @param {number} offX @param {number} offY @param {number} w @param {number} h @param {number} cell */
function drawGrid(offX, offY, w, h, cell) {
    ctx.save();

    const boardW = w * cell;
    const boardH = h * cell;

    ctx.beginPath();
    ctx.rect(offX, offY, boardW, boardH);
    ctx.clip();

    const targetPx = 22;
    const step = Math.max(1, Math.round(targetPx / cell));

    ctx.strokeStyle = "rgba(0,240,255,0.06)";
    ctx.lineWidth = 1;

    for (let x = 0; x <= w; x += step) {
        const px = offX + x * cell + 0.5;
        ctx.beginPath();
        ctx.moveTo(px, offY + 0.5);
        ctx.lineTo(px, offY + boardH - 0.5);
        ctx.stroke();
    }

    for (let y = 0; y <= h; y += step) {
        const py = offY + y * cell + 0.5;
        ctx.beginPath();
        ctx.moveTo(offX + 0.5, py);
        ctx.lineTo(offX + boardW - 0.5, py);
        ctx.stroke();
    }

    const majorEvery = 5;
    ctx.strokeStyle = "rgba(0,240,255,0.10)";
    for (let x = 0; x <= w; x += step * majorEvery) {
        const px = offX + x * cell + 0.5;
        ctx.beginPath();
        ctx.moveTo(px, offY + 0.5);
        ctx.lineTo(px, offY + boardH - 0.5);
        ctx.stroke();
    }

    for (let y = 0; y <= h; y += step * majorEvery) {
        const py = offY + y * cell + 0.5;
        ctx.beginPath();
        ctx.moveTo(offX + 0.5, py);
        ctx.lineTo(offX + boardW - 0.5, py);
        ctx.stroke();
    }

    ctx.restore();
}

function glowRect(x, y, w, h, color, blur) {
    ctx.save();
    ctx.shadowColor = color;
    ctx.shadowBlur = blur;
    ctx.strokeStyle = color;
    ctx.lineWidth = 2;
    ctx.strokeRect(x + 1, y + 1, w - 2, h - 2);
    ctx.restore();
}

/** @param {Trail[]} walls @param {number} offX @param {number} offY @param {number} cell */
function drawWalls(walls, offX, offY, cell) {
    if (!Array.isArray(walls) || walls.length === 0) return;

    const wallsByOwner = walls
        .filter(t => t.position)
        .reduce((map, t) => {
            const owner = t.ownerId || "unknown";
            return new Map([...map, [owner, [...(map.get(owner) ?? []), t.position]]]);
        }, new Map());

    [...wallsByOwner.entries()].forEach(([owner, pts]) => {
        const isP1 = owner === "p1";
        const core = isP1 ? "rgba(0,240,255,0.95)" : "rgba(255,59,209,0.95)";
        const glow = isP1 ? "rgba(0,240,255,0.55)" : "rgba(255,59,209,0.55)";

        const lineW = Math.max(1, Math.floor(cell * 0.22));
        const glowW = lineW + Math.max(3, Math.floor(cell * 0.42));

        ctx.save();
        ctx.lineCap = "round";
        ctx.lineJoin = "round";
        ctx.strokeStyle = glow;
        ctx.lineWidth = glowW;
        ctx.shadowColor = glow;
        ctx.shadowBlur = Math.max(8, Math.floor(cell * 1.0));
        strokeOrderedSegments(pts, offX, offY, cell);
        ctx.restore();

        ctx.save();
        ctx.lineCap = "round";
        ctx.lineJoin = "round";
        ctx.strokeStyle = core;
        ctx.lineWidth = lineW;
        strokeOrderedSegments(pts, offX, offY, cell);
        ctx.restore();
    });
}

/**
 * @param {number} offX @param {number} offY @param {number} cell @param {Position} p
 * @returns {{ x: number, y: number }}
 */
function center(offX, offY, cell, p) {
    return {
        x: offX + p.x * cell + cell / 2,
        y: offY + p.y * cell + cell / 2
    };
}

/** @param {Position[]} pts @param {number} offX @param {number} offY @param {number} cell */
function strokeOrderedSegments(pts, offX, offY, cell) {
    ctx.beginPath();

    for (let i = 1; i < pts.length; i++) {
        const a = pts[i - 1];
        const b = pts[i];

        const dx = Math.abs(a.x - b.x);
        const dy = Math.abs(a.y - b.y);

        if (dx + dy !== 1) continue;

        const centerA = center(offX, offY, cell, a);
        const centerB = center(offX, offY, cell, b);
        ctx.moveTo(centerA.x, centerA.y);
        ctx.lineTo(centerB.x, centerB.y);
    }

    ctx.stroke();
}

/** @param {Player[]} players @param {number} offX @param {number} offY @param {number} cell */
function drawPlayers(players, offX, offY, cell) {
    if (!Array.isArray(players)) return;

    players.filter(pl => pl.position).forEach((pl) => {
        const p = pl.position;

        const isP1 = pl.id === "p1";
        const base = isP1 ? "#00f0ff" : "#ff3bd1";
        const glow = isP1 ? "rgba(0,240,255,0.75)" : "rgba(255,59,209,0.75)";
        const dead = "rgba(255,255,255,0.22)";

        const x = offX + p.x * cell;
        const y = offY + p.y * cell;
        const cx = x + cell / 2;
        const cy = y + cell / 2;
        const r = Math.max(4, cell * 0.42);

        ctx.save();
        ctx.shadowColor = pl.alive ? glow : dead;
        ctx.shadowBlur = Math.max(14, Math.floor(cell * 1.8));
        ctx.fillStyle = pl.alive ? base : dead;
        ctx.beginPath();
        ctx.arc(cx, cy, r, 0, Math.PI * 2);
        ctx.fill();
        ctx.restore();

        ctx.save();
        ctx.fillStyle = "rgba(255,255,255,0.85)";
        ctx.beginPath();
        ctx.arc(cx - r * 0.15, cy - r * 0.15, Math.max(2, r * 0.28), 0, Math.PI * 2);
        ctx.fill();
        ctx.restore();

        const { dx, dy } = DIRECTION_VECTORS[pl.direction] ?? { dx: 0, dy: 0 };

        ctx.save();
        ctx.strokeStyle = "rgba(255,255,255,0.9)";
        ctx.lineWidth = Math.max(2, Math.floor(cell * 0.12));
        ctx.lineCap = "round";
        ctx.beginPath();
        ctx.moveTo(cx, cy);
        ctx.lineTo(cx + dx * r * 0.9, cy + dy * r * 0.9);
        ctx.stroke();
        ctx.restore();
    });
}

function overlayText(x, y, w, h, text) {
    ctx.save();
    ctx.fillStyle = "rgba(0,0,0,0.45)";
    ctx.fillRect(x, y, w, h);

    ctx.font = "700 34px system-ui, sans-serif";
    ctx.textAlign = "center";
    ctx.textBaseline = "middle";
    ctx.fillStyle = "rgba(230,245,255,0.92)";
    ctx.shadowColor = "rgba(0,240,255,0.35)";
    ctx.shadowBlur = 18;
    ctx.fillText(text, x + w / 2, y + h / 2);
    ctx.restore();
}
