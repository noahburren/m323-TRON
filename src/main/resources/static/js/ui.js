import { wsUrl } from "./config.js";

export const canvas = document.getElementById("c");
export const ctx = canvas.getContext("2d");

export const wsDot = document.getElementById("wsDot");
export const statusEl = document.getElementById("status");
export const tickEl = document.getElementById("tick");
export const gridInfoEl = document.getElementById("gridInfo");
export const playerInfoEl = document.getElementById("playerInfo");
export const logEl = document.getElementById("log");
export const wsUrlEl = document.getElementById("wsUrl");

wsUrlEl.textContent = wsUrl;

export function log(line) {
    logEl.textContent += line + "\n";
    logEl.scrollTop = logEl.scrollHeight;
}

export function fitCanvas() {
    const rect = canvas.getBoundingClientRect();
    const dpr = Math.max(1, Math.floor(window.devicePixelRatio || 1));
    canvas.width = Math.floor(rect.width * dpr);
    canvas.height = Math.floor(rect.height * dpr);
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
}

export function updateHud(state) {
    statusEl.textContent = state.status ?? "-";
    tickEl.textContent = state.tick ?? "-";
    gridInfoEl.textContent = `${state.width} x ${state.height}`;
    playerInfoEl.textContent = Array.isArray(state.players)
        ? state.players.map(p => `${p.id}:${p.alive ? "alive" : "dead"}`).join("  ")
        : "-";
}