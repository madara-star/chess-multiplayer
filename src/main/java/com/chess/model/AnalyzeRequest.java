package com.chess.model;

import java.util.List;

public class AnalyzeRequest {
    private List<MoveEntry> moves;

    public List<MoveEntry> getMoves() { return moves; }
    public void setMoves(List<MoveEntry> moves) { this.moves = moves; }

    public static class MoveEntry {
        private int fr, fc, tr, tc;
        private String promo = "q";
        private String san;
        private boolean wasWhite;

        public int getFr() { return fr; }
        public void setFr(int fr) { this.fr = fr; }
        public int getFc() { return fc; }
        public void setFc(int fc) { this.fc = fc; }
        public int getTr() { return tr; }
        public void setTr(int tr) { this.tr = tr; }
        public int getTc() { return tc; }
        public void setTc(int tc) { this.tc = tc; }
        public String getPromo() { return promo; }
        public void setPromo(String promo) { this.promo = promo; }
        public String getSan() { return san; }
        public void setSan(String san) { this.san = san; }
        public boolean isWasWhite() { return wasWhite; }
        public void setWasWhite(boolean wasWhite) { this.wasWhite = wasWhite; }
    }
}
