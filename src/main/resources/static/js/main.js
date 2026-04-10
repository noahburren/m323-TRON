import { fitCanvas } from "./ui.js";
import { createSocket } from "./websocket.js";
import { registerControls } from "./controls.js";

fitCanvas();
window.addEventListener("resize", fitCanvas);

const ws = createSocket();
registerControls(ws);