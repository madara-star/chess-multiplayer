package com.chess.model;

import java.util.List;

public class AnalyzeResponse {
    private List<MoveResult> results;
    private int whiteAccuracy;
    private int blackAccuracy;

    public List<MoveResult> getResults() { return results; }
    public void setResults(List<MoveResult> results) { this.results = results; }
    public int getWhiteAccuracy() { return whiteAccuracy; }
    public void setWhiteAccuracy(int whiteAccuracy) { this.whiteAccuracy = whiteAccuracy; }
    public int getBlackAccuracy() { return blackAccuracy; }
    public void setBlackAccuracy(int blackAccuracy) { this.blackAccuracy = blackAccuracy; }

    public static class MoveResult {
        private String san;
        private int loss;
        // best | good | inacc | mistake | blunder
        private String classification;
        private boolean wasWhite;

        public MoveResult(String san, int loss, String classification, boolean wasWhite) {
            this.san = san; this.loss = loss;
            this.classification = classification; this.wasWhite = wasWhite;
        }

        public String getSan() { return san; }
        public int getLoss() { return loss; }
        public String getClassification() { return classification; }
        public boolean isWasWhite() { return wasWhite; }
    }
}
