package com.chess.model;

public class BotMoveRequest {
    // 8x8 board: uppercase = white, lowercase = black, "" = empty
    private String[][] board;
    private boolean whiteToMove;
    private boolean wKS = true, wQS = true, bKS = true, bQS = true;
    private int epRow = -1, epCol = -1;
    private String difficulty = "medium"; // easy | medium | hard

    public String[][] getBoard() { return board; }
    public void setBoard(String[][] board) { this.board = board; }
    public boolean isWhiteToMove() { return whiteToMove; }
    public void setWhiteToMove(boolean whiteToMove) { this.whiteToMove = whiteToMove; }
    public boolean isWKS() { return wKS; }
    public void setWKS(boolean wKS) { this.wKS = wKS; }
    public boolean isWQS() { return wQS; }
    public void setWQS(boolean wQS) { this.wQS = wQS; }
    public boolean isBKS() { return bKS; }
    public void setBKS(boolean bKS) { this.bKS = bKS; }
    public boolean isBQS() { return bQS; }
    public void setBQS(boolean bQS) { this.bQS = bQS; }
    public int getEpRow() { return epRow; }
    public void setEpRow(int epRow) { this.epRow = epRow; }
    public int getEpCol() { return epCol; }
    public void setEpCol(int epCol) { this.epCol = epCol; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
}
