import { wsUrl } from "./config.js";
import { appState } from "./state.js";
import { wsDot, log, updateHud } from "./ui.js";
import { render } from "./renderer.js";

export function createSocket() {
    const ws = new WebSocket(wsUrl);

    ws.onopen = () => {
        wsDot.classList.add("ok");
        wsDot.classList.remove("bad");
        log("OPEN");
    };

    ws.onclose = () => {
        wsDot.classList.remove("ok");
        wsDot.classList.add("bad");
        log("CLOSE");
    };

    ws.onerror = (e) => {
        wsDot.classList.remove("ok");
        wsDot.classList.add("bad");
        log("ERROR " + e.type);
    };

    ws.onmessage = (e) => {
        try {
            const msg = JSON.parse(e.data);
            const state = msg.state ?? msg;
            const winner = msg.winner ?? null;

            appState.game = state;
            appState.winner = winner;

            updateHud(state);
            render(state, winner);
        } catch (err) {
            log("PARSE ERROR: " + err);
        }
    };

    return ws;
}

export function sendTurnFor(ws, playerId, dir) {
    if (ws.readyState !== WebSocket.OPEN) return;
    ws.send(JSON.stringify({ type: "TURN", playerId, dir }));
}