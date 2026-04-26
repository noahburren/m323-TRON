import "./types.js";
import { wsUrl } from "./config.js";
import { appState } from "./state.js";
import { wsDot, log, updateHud } from "./ui.js";
import { render } from "./renderer.js";
import { pipe } from "./utils.js";

/** @param {string} raw @returns {object} */
const parseMessage = (raw) => JSON.parse(raw);

/** @param {object} msg @returns {{ state: GameState, winner: string|null }} */
const extractPayload = (msg) => ({ state: msg.state ?? msg, winner: msg.winner ?? null });

/** @param {{ state: GameState, winner: string|null }} payload */
const applyAndRender = ({ state, winner }) => {
    appState.set({ game: state, winner });
    updateHud(state);
    render(state, winner);
};

const processMessage = pipe(parseMessage, extractPayload, applyAndRender);

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
            processMessage(e.data);
        } catch (err) {
            log("PARSE ERROR: " + err);
        }
    };

    return ws;
}

/** @param {WebSocket} ws @param {string} playerId @param {string} dir */
export function sendTurnFor(ws, playerId, dir) {
    if (ws.readyState !== WebSocket.OPEN) return;
    ws.send(JSON.stringify({ type: "TURN", playerId, dir }));
}
