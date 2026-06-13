package com.chess.model;

public class BotMoveResponse {
    private int fromRow, fromCol, toRow, toCol;
    private int eval;
    private String error;

    public BotMoveResponse() {}
    public BotMoveResponse(int fr, int fc, int tr, int tc, int eval) {
        this.fromRow = fr; this.fromCol = fc; this.toRow = tr; this.toCol = tc; this.eval = eval;
    }

    public int getFromRow() { return fromRow; }
    public void setFromRow(int fromRow) { this.fromRow = fromRow; }
    public int getFromCol() { return fromCol; }
    public void setFromCol(int fromCol) { this.fromCol = fromCol; }
    public int getToRow() { return toRow; }
    public void setToRow(int toRow) { this.toRow = toRow; }
    public int getToCol() { return toCol; }
    public void setToCol(int toCol) { this.toCol = toCol; }
    public int getEval() { return eval; }
    public void setEval(int eval) { this.eval = eval; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
