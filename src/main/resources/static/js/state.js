import "./types.js";

/**
 * Closure-based app state — the inner object is never exposed directly,
 * so callers cannot mutate it; they must go through set().
 * @returns {{ get: () => AppState, set: (next: AppState) => void }}
 */
function createAppState() {
    let _state = { winner: null, game: null };
    return {
        get: () => _state,
        set: (next) => { _state = next; }
    };
}

export const appState = createAppState();
