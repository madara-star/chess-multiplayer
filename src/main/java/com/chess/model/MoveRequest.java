package com.chess.model;

public class MoveRequest {
    private String playerId;
    private int fromRow;
    private int fromCol;
    private int toRow;
    private int toCol;
    private char promotion = 'q';

    public String getPlayerId()  { return playerId; }
    public int    getFromRow()   { return fromRow; }
    public int    getFromCol()   { return fromCol; }
    public int    getToRow()     { return toRow; }
    public int    getToCol()     { return toCol; }
    public char   getPromotion() { return promotion; }

    public void setPlayerId(String playerId)   { this.playerId = playerId; }
    public void setFromRow(int fromRow)        { this.fromRow = fromRow; }
    public void setFromCol(int fromCol)        { this.fromCol = fromCol; }
    public void setToRow(int toRow)            { this.toRow = toRow; }
    public void setToCol(int toCol)            { this.toCol = toCol; }
    public void setPromotion(char promotion)   { this.promotion = promotion; }
}
