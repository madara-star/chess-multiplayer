package com.chess.model;

import com.chess.engine.GameStatus;
import java.util.List;

/**
 * Broadcast over WebSocket to both players after every state change.
 */
public class GameStateMessage {

    private String      type;           // GAME_STATE | GAME_START | ERROR | PLAYER_LEFT
    private String      gameId;
    private String[][]  board;          // 8x8 piece chars (empty string = empty)
    private boolean     whiteToMove;
    private GameStatus  status;
    private int[]       lastMove;       // [fr,fc,tr,tc] or null
    private List<String> moveHistory;
    private String      message;        // for ERROR type
    private int         moveCount;

    // ── Factories ─────────────────────────────────────────────────────────────

    public static GameStateMessage gameState(Game game) {
        GameStateMessage m = new GameStateMessage();
        m.type        = "GAME_STATE";
        m.gameId      = game.getId();
        m.board       = game.getBoardArray();
        m.whiteToMove = game.isWhiteToMove();
        m.status      = game.getStatus();
        m.lastMove    = game.getLastMoveSquares();
        m.moveHistory = game.getMoveHistory();
        m.moveCount   = game.getMoveHistory().size();
        return m;
    }

    public static GameStateMessage gameStart(Game game) {
        GameStateMessage m = gameState(game);
        m.type = "GAME_START";
        return m;
    }

    public static GameStateMessage error(String gameId, String msg) {
        GameStateMessage m = new GameStateMessage();
        m.type    = "ERROR";
        m.gameId  = gameId;
        m.message = msg;
        return m;
    }

    public static GameStateMessage playerLeft(String gameId) {
        GameStateMessage m = new GameStateMessage();
        m.type   = "PLAYER_LEFT";
        m.gameId = gameId;
        return m;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String      getType()        { return type; }
    public String      getGameId()      { return gameId; }
    public String[][]  getBoard()       { return board; }
    public boolean     isWhiteToMove()  { return whiteToMove; }
    public GameStatus  getStatus()      { return status; }
    public int[]       getLastMove()    { return lastMove; }
    public List<String> getMoveHistory(){ return moveHistory; }
    public String      getMessage()     { return message; }
    public int         getMoveCount()   { return moveCount; }
}
