package com.chess.controller;

import com.chess.engine.BotEngine;
import com.chess.model.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/bot")
public class BotController {

    // ── Best Move ─────────────────────────────────────────────────────────────
    @PostMapping("/best-move")
    public BotMoveResponse bestMove(@RequestBody BotMoveRequest req) {
        char[][] board = toCharBoard(req.getBoard());
        BotEngine.State state = new BotEngine.State(
            req.isWhiteToMove(), req.isWKS(), req.isWQS(), req.isBKS(), req.isBQS(),
            req.getEpRow(), req.getEpCol()
        );
        BotEngine.BestMove best = BotEngine.bestMove(board, state, req.getDifficulty());
        if (best == null) {
            BotMoveResponse err = new BotMoveResponse();
            err.setError("No legal moves");
            return err;
        }
        return new BotMoveResponse(best.fromRow, best.fromCol, best.toRow, best.toCol, best.eval);
    }

    // ── Game Analysis ─────────────────────────────────────────────────────────
    @PostMapping("/analyze")
    public AnalyzeResponse analyze(@RequestBody AnalyzeRequest req) {
        List<AnalyzeRequest.MoveEntry> moves = req.getMoves();
        List<AnalyzeResponse.MoveResult> results = new ArrayList<>();

        // Start from the initial position
        char[][] board = initialBoard();
        BotEngine.State state = new BotEngine.State(true, true, true, true, true, -1, -1);

        for (AnalyzeRequest.MoveEntry mv : moves) {
            boolean isWhite = state.wt;
            char promo = (mv.getPromo() != null && !mv.getPromo().isEmpty()) ? mv.getPromo().charAt(0) : 'q';

            // Find engine's best at depth 2 and its evaluation
            BotEngine.BestMove best = BotEngine.bestMoveAtDepth(board, state, 2);
            int bestEval = (best != null) ? best.eval : (isWhite ? -999999 : 999999);

            // Apply actual move and evaluate resulting position
            BotEngine.MoveApplied applied = BotEngine.applyMove(board, state, mv.getFr(), mv.getFc(), mv.getTr(), mv.getTc(), promo);
            int actualEval = BotEngine.evaluate(applied.board);

            // Centipawn loss from mover's perspective
            int loss = isWhite ? (bestEval - actualEval) : (actualEval - bestEval);
            loss = Math.max(0, loss);

            results.add(new AnalyzeResponse.MoveResult(
                mv.getSan(), loss, classify(loss), isWhite
            ));

            board = applied.board;
            state = applied.state;
        }

        AnalyzeResponse response = new AnalyzeResponse();
        response.setResults(results);
        response.setWhiteAccuracy(accuracy(results, true));
        response.setBlackAccuracy(accuracy(results, false));
        return response;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private char[][] toCharBoard(String[][] src) {
        char[][] board = new char[8][8];
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                String s = (src != null && src[r] != null) ? src[r][c] : null;
                board[r][c] = (s == null || s.isEmpty()) ? '.' : s.charAt(0);
            }
        return board;
    }

    private char[][] initialBoard() {
        String[] rows = {
            "rnbqkbnr", "pppppppp", "........", "........",
            "........", "........", "PPPPPPPP", "RNBQKBNR"
        };
        char[][] b = new char[8][8];
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                b[r][c] = rows[r].charAt(c);
        return b;
    }

    private String classify(int loss) {
        if (loss <= 20)  return "best";
        if (loss <= 60)  return "good";
        if (loss <= 150) return "inacc";
        if (loss <= 300) return "mistake";
        return "blunder";
    }

    private int accuracy(List<AnalyzeResponse.MoveResult> results, boolean forWhite) {
        List<AnalyzeResponse.MoveResult> mine = new ArrayList<>();
        for (AnalyzeResponse.MoveResult r : results)
            if (r.isWasWhite() == forWhite) mine.add(r);
        if (mine.isEmpty()) return 100;
        double total = mine.stream().mapToInt(AnalyzeResponse.MoveResult::getLoss).sum();
        return (int) Math.max(0, Math.round(100 - total / mine.size() / 10.0));
    }
}
