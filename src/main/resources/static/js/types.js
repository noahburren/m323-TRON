/**
 * @typedef {{ x: number, y: number }} Position
 * @typedef {{ id: string, position: Position, direction: string, alive: boolean }} Player
 * @typedef {{ position: Position, ownerId: string }} Trail
 * @typedef {{ tick: number, width: number, height: number, players: Player[], walls: Trail[], status: string }} GameState
 * @typedef {{ game: GameState|null, winner: string|null }} AppState
 */
