import { sendTurnFor } from "./websocket.js";

export function registerControls(ws) {
    window.addEventListener("keydown", (ev) => {
        const wasd = { w: "UP", a: "LEFT", s: "DOWN", d: "RIGHT" };
        const arrows = { ArrowUp: "UP", ArrowLeft: "LEFT", ArrowDown: "DOWN", ArrowRight: "RIGHT" };

        if (wasd[ev.key]) {
            ev.preventDefault();
            sendTurnFor(ws, "p1", wasd[ev.key]);
            return;
        }

        if (arrows[ev.key]) {
            ev.preventDefault();
            sendTurnFor(ws, "p2", arrows[ev.key]);
        }
    });
}