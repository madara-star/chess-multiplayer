package com.chess.engine;

public class MoveResult {
    private final char captured;
    private final boolean castled;
    private final boolean enPassant;
    private final boolean promoted;
    private final String  san;
    private final int[]   squares;   // [fr, fc, tr, tc]
    private final GameStatus statusAfter;

    public MoveResult(char captured, boolean castled, boolean enPassant, boolean promoted,
                      String san, int[] squares, GameStatus statusAfter) {
        this.captured    = captured;
        this.castled     = castled;
        this.enPassant   = enPassant;
        this.promoted    = promoted;
        this.san         = san;
        this.squares     = squares;
        this.statusAfter = statusAfter;
    }

    public char     getCaptured()    { return captured; }
    public boolean  isCastled()      { return castled; }
    public boolean  isEnPassant()    { return enPassant; }
    public boolean  isPromoted()     { return promoted; }
    public String   getSan()         { return san; }
    public int[]    getSquares()     { return squares; }
    public GameStatus getStatusAfter() { return statusAfter; }
}
