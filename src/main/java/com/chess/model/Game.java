package com.chess.model;

import com.chess.engine.ChessEngine;
import com.chess.engine.GameStatus;
import com.chess.engine.MoveResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Game {

    private final String id;
    private final ChessEngine engine = new ChessEngine();
    private final String whitePlayerId;
    private volatile String blackPlayerId;
    private volatile GameStatus status = GameStatus.ONGOING;
    private final List<String> moveHistory = Collections.synchronizedList(new ArrayList<>());
    private volatile int[] lastMoveSquares; // [fr, fc, tr, tc]
    private final Instant createdAt = Instant.now();

    public Game(String id, String whitePlayerId) {
        this.id = id;
        this.whitePlayerId = whitePlayerId;
    }

    // ── Synchronized move execution ───────────────────────────────────────────

    /**
     * Attempts to make a move on behalf of playerId.
     * Returns the MoveResult or null if the move is invalid / not that player's turn.
     */
    public synchronized MoveResult makeMove(String playerId, int fr, int fc, int tr, int tc, char promotion) {
        if (status == GameStatus.WHITE_WINS || status == GameStatus.BLACK_WINS
                || status == GameStatus.STALEMATE || status == GameStatus.DRAW) return null;

        boolean isWhiteTurn = engine.isWhiteToMove();
        String  expected    = isWhiteTurn ? whitePlayerId : blackPlayerId;
        if (!playerId.equals(expected)) return null;

        MoveResult result = engine.makeMove(fr, fc, tr, tc, promotion);
        if (result == null) return null;

        moveHistory.add(result.getSan());
        lastMoveSquares = new int[]{fr, fc, tr, tc};
        status = result.getStatusAfter();
        return result;
    }

    public boolean isFull() { return blackPlayerId != null; }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String    getId()              { return id; }
    public String    getWhitePlayerId()   { return whitePlayerId; }
    public String    getBlackPlayerId()   { return blackPlayerId; }
    public GameStatus getStatus()         { return status; }
    public List<String> getMoveHistory()  { return Collections.unmodifiableList(moveHistory); }
    public int[]     getLastMoveSquares() { return lastMoveSquares; }
    public Instant   getCreatedAt()       { return createdAt; }

    public String[][] getBoardArray() { return engine.getBoardArray(); }
    public boolean    isWhiteToMove() { return engine.isWhiteToMove(); }

    /**
     * Returns all legal destination squares for the piece at (row, col),
     * only if it is playerId's turn.
     */
    public List<int[]> getLegalMoves(String playerId, int row, int col) {
        boolean isWhiteTurn = engine.isWhiteToMove();
        String expected = isWhiteTurn ? whitePlayerId : blackPlayerId;
        if (!playerId.equals(expected)) return Collections.emptyList();
        return engine.getLegalMoves(row, col);
    }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setBlackPlayerId(String id) { this.blackPlayerId = id; }
}
