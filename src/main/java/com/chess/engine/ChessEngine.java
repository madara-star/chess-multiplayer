package com.chess.engine;

import java.util.*;

/**
 * Full chess engine.
 * Board: board[row][col], row 0 = rank 8 (black back rank), row 7 = rank 1 (white back rank)
 * Piece encoding: uppercase = white, lowercase = black, '.' = empty
 * P/p=pawn  N/n=knight  B/b=bishop  R/r=rook  Q/q=queen  K/k=king
 */
public class ChessEngine {

    private char[][] board = new char[8][8];
    private boolean whiteToMove = true;
    private boolean whiteKingSide  = true;
    private boolean whiteQueenSide = true;
    private boolean blackKingSide  = true;
    private boolean blackQueenSide = true;
    private int epRow = -1, epCol = -1;   // en-passant target square
    private int halfMoveClock = 0;
    private int fullMoveNumber = 1;

    public ChessEngine() { initBoard(); }

    // ── Init ──────────────────────────────────────────────────────────────────

    private void initBoard() {
        String[] ranks = {
            "rnbqkbnr",
            "pppppppp",
            "........",
            "........",
            "........",
            "........",
            "PPPPPPPP",
            "RNBQKBNR"
        };
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                board[r][c] = ranks[r].charAt(c);
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public boolean isWhiteToMove() { return whiteToMove; }

    /** All legal destination squares for the piece at (row, col). */
    public List<int[]> getLegalMoves(int row, int col) {
        char piece = board[row][col];
        if (piece == '.') return Collections.emptyList();
        boolean isWhite = Character.isUpperCase(piece);
        if (isWhite != whiteToMove) return Collections.emptyList();

        List<int[]> pseudo = getPseudoLegal(row, col);
        List<int[]> legal  = new ArrayList<>();
        for (int[] to : pseudo)
            if (!wouldLeaveKingInCheck(row, col, to[0], to[1]))
                legal.add(to);
        return legal;
    }

    /** Returns all legal moves as [fromRow, fromCol, toRow, toCol] for the current player. */
    public List<int[]> getAllLegalMoves() {
        List<int[]> all = new ArrayList<>();
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                char p = board[r][c];
                if (p != '.' && Character.isUpperCase(p) == whiteToMove) {
                    for (int[] to : getLegalMoves(r, c))
                        all.add(new int[]{r, c, to[0], to[1]});
                }
            }
        return all;
    }

    /**
     * Attempt to make a move. Returns null if illegal.
     * promotion: 'q','r','b','n' (or '\0' for auto-queen)
     */
    public MoveResult makeMove(int fr, int fc, int tr, int tc, char promotion) {
        // Validate legality
        List<int[]> legal = getLegalMoves(fr, fc);
        boolean isLegal = legal.stream().anyMatch(m -> m[0] == tr && m[1] == tc);
        if (!isLegal) return null;

        // Compute SAN before applying
        String san = computeSAN(fr, fc, tr, tc, promotion);

        char piece    = board[fr][fc];
        char captured = board[tr][tc];
        char type     = Character.toLowerCase(piece);
        boolean white = Character.isUpperCase(piece);

        // En-passant capture
        int epCaptureRow = -1, epCaptureCol = -1;
        char epCapturedPiece = '.';
        if (type == 'p' && fc != tc && captured == '.') {
            epCaptureRow = fr;
            epCaptureCol = tc;
            epCapturedPiece = board[epCaptureRow][epCaptureCol];
            board[epCaptureRow][epCaptureCol] = '.';
            captured = epCapturedPiece;
        }

        board[tr][tc] = piece;
        board[fr][fc] = '.';

        // Castling – move rook
        boolean castled = false;
        if (type == 'k' && Math.abs(tc - fc) == 2) {
            castled = true;
            if (tc > fc) { board[fr][fc + 1] = board[fr][7]; board[fr][7] = '.'; }
            else         { board[fr][fc - 1] = board[fr][0]; board[fr][0] = '.'; }
            if (white) { whiteKingSide = false; whiteQueenSide = false; }
            else       { blackKingSide = false; blackQueenSide = false; }
        }

        // Update castling rights for king/rook moves
        if (type == 'k') {
            if (white) { whiteKingSide = false; whiteQueenSide = false; }
            else       { blackKingSide = false; blackQueenSide = false; }
        }
        if (type == 'r') {
            if (white) { if (fc == 7) whiteKingSide = false; if (fc == 0) whiteQueenSide = false; }
            else       { if (fc == 7) blackKingSide  = false; if (fc == 0) blackQueenSide  = false; }
        }
        // If a rook is captured, revoke its castling right
        if (tr == 7 && tc == 7) whiteKingSide  = false;
        if (tr == 7 && tc == 0) whiteQueenSide = false;
        if (tr == 0 && tc == 7) blackKingSide   = false;
        if (tr == 0 && tc == 0) blackQueenSide  = false;

        // Pawn promotion
        boolean promoted = false;
        if (type == 'p' && (tr == 0 || tr == 7)) {
            char prom = (promotion == 0) ? 'q' : Character.toLowerCase(promotion);
            board[tr][tc] = white ? Character.toUpperCase(prom) : prom;
            promoted = true;
        }

        // En-passant target for next move
        epRow = -1; epCol = -1;
        if (type == 'p' && Math.abs(tr - fr) == 2) {
            epRow = (fr + tr) / 2;
            epCol = fc;
        }

        whiteToMove = !whiteToMove;
        if (captured != '.' || type == 'p') halfMoveClock = 0; else halfMoveClock++;
        if (!white) fullMoveNumber++;

        // Append check/mate symbol
        GameStatus status = getStatus();
        if (status == GameStatus.WHITE_WINS || status == GameStatus.BLACK_WINS) san += "#";
        else if (status == GameStatus.CHECK) san += "+";

        return new MoveResult(captured, castled, epCaptureRow >= 0, promoted, san,
                              new int[]{fr, fc, tr, tc}, status);
    }

    /** Current game status (call after makeMove). */
    public GameStatus getStatus() {
        boolean anyLegal = false;
        outer:
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                char p = board[r][c];
                if (p != '.' && Character.isUpperCase(p) == whiteToMove)
                    if (!getLegalMoves(r, c).isEmpty()) { anyLegal = true; break outer; }
            }

        if (!anyLegal) {
            if (isInCheck(whiteToMove)) return whiteToMove ? GameStatus.BLACK_WINS : GameStatus.WHITE_WINS;
            return GameStatus.STALEMATE;
        }
        if (halfMoveClock >= 100) return GameStatus.DRAW;
        if (isInCheck(whiteToMove)) return GameStatus.CHECK;
        return GameStatus.ONGOING;
    }

    /** Returns the board as a 2-D String array (empty string = empty square). */
    public String[][] getBoardArray() {
        String[][] out = new String[8][8];
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                out[r][c] = board[r][c] == '.' ? "" : String.valueOf(board[r][c]);
        return out;
    }

    // ── Check detection ───────────────────────────────────────────────────────

    private boolean isInCheck(boolean white) {
        char king = white ? 'K' : 'k';
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                if (board[r][c] == king) return isAttackedBy(r, c, !white);
        return false;
    }

    private boolean isAttackedBy(int row, int col, boolean byWhite) {
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                char p = board[r][c];
                if (p == '.' || Character.isUpperCase(p) != byWhite) continue;
                if (attacks(r, c, row, col)) return true;
            }
        return false;
    }

    private boolean attacks(int fr, int fc, int tr, int tc) {
        char p    = board[fr][fc];
        char type = Character.toLowerCase(p);
        boolean w = Character.isUpperCase(p);
        int dr = tr - fr, dc = tc - fc;
        return switch (type) {
            case 'p'  -> dr == (w ? -1 : 1) && Math.abs(dc) == 1;
            case 'n'  -> (Math.abs(dr) == 2 && Math.abs(dc) == 1) || (Math.abs(dr) == 1 && Math.abs(dc) == 2);
            case 'b'  -> Math.abs(dr) == Math.abs(dc) && dr != 0 && pathClear(fr, fc, tr, tc);
            case 'r'  -> (dr == 0 || dc == 0) && pathClear(fr, fc, tr, tc);
            case 'q'  -> ((dr == 0 || dc == 0) || Math.abs(dr) == Math.abs(dc)) && pathClear(fr, fc, tr, tc);
            case 'k'  -> Math.abs(dr) <= 1 && Math.abs(dc) <= 1;
            default   -> false;
        };
    }

    private boolean pathClear(int fr, int fc, int tr, int tc) {
        int dr = Integer.signum(tr - fr), dc = Integer.signum(tc - fc);
        int r = fr + dr, c = fc + dc;
        while (r != tr || c != tc) {
            if (board[r][c] != '.') return false;
            r += dr; c += dc;
        }
        return true;
    }

    // ── Pseudo-legal move generation ──────────────────────────────────────────

    private List<int[]> getPseudoLegal(int row, int col) {
        char p    = board[row][col];
        char type = Character.toLowerCase(p);
        boolean w = Character.isUpperCase(p);
        List<int[]> moves = new ArrayList<>();
        switch (type) {
            case 'p' -> addPawnMoves(row, col, w, moves);
            case 'n' -> addKnightMoves(row, col, w, moves);
            case 'b' -> addSliding(row, col, w, moves, new int[][]{{1,1},{1,-1},{-1,1},{-1,-1}});
            case 'r' -> addSliding(row, col, w, moves, new int[][]{{1,0},{-1,0},{0,1},{0,-1}});
            case 'q' -> addSliding(row, col, w, moves, new int[][]{{1,1},{1,-1},{-1,1},{-1,-1},{1,0},{-1,0},{0,1},{0,-1}});
            case 'k' -> addKingMoves(row, col, w, moves);
        }
        return moves;
    }

    private void addPawnMoves(int row, int col, boolean white, List<int[]> out) {
        int dir = white ? -1 : 1;
        int startRow = white ? 6 : 1;

        // One forward
        if (inBounds(row + dir, col) && board[row + dir][col] == '.') {
            out.add(new int[]{row + dir, col});
            // Two forward from start
            if (row == startRow && board[row + 2*dir][col] == '.')
                out.add(new int[]{row + 2*dir, col});
        }
        // Diagonal captures + en-passant
        for (int dc : new int[]{-1, 1}) {
            int nr = row + dir, nc = col + dc;
            if (!inBounds(nr, nc)) continue;
            char t = board[nr][nc];
            boolean isEnPassant = nr == epRow && nc == epCol;
            if ((t != '.' && Character.isUpperCase(t) != white) || isEnPassant)
                out.add(new int[]{nr, nc});
        }
    }

    private void addKnightMoves(int row, int col, boolean white, List<int[]> out) {
        for (int[] d : new int[][]{{-2,-1},{-2,1},{-1,-2},{-1,2},{1,-2},{1,2},{2,-1},{2,1}}) {
            int nr = row + d[0], nc = col + d[1];
            if (inBounds(nr, nc)) {
                char t = board[nr][nc];
                if (t == '.' || Character.isUpperCase(t) != white)
                    out.add(new int[]{nr, nc});
            }
        }
    }

    private void addSliding(int row, int col, boolean white, List<int[]> out, int[][] dirs) {
        for (int[] d : dirs) {
            int nr = row + d[0], nc = col + d[1];
            while (inBounds(nr, nc)) {
                char t = board[nr][nc];
                if (t == '.') { out.add(new int[]{nr, nc}); }
                else {
                    if (Character.isUpperCase(t) != white) out.add(new int[]{nr, nc});
                    break;
                }
                nr += d[0]; nc += d[1];
            }
        }
    }

    private void addKingMoves(int row, int col, boolean white, List<int[]> out) {
        for (int[] d : new int[][]{{-1,-1},{-1,0},{-1,1},{0,-1},{0,1},{1,-1},{1,0},{1,1}}) {
            int nr = row + d[0], nc = col + d[1];
            if (inBounds(nr, nc)) {
                char t = board[nr][nc];
                if (t == '.' || Character.isUpperCase(t) != white)
                    out.add(new int[]{nr, nc});
            }
        }
        // Castling (pseudo-legal only; check-through-squares handled in wouldLeaveKingInCheck)
        boolean enemy = !white;
        if (white) {
            if (whiteKingSide  && board[7][5]=='.' && board[7][6]=='.'
                    && !isAttackedBy(7,4,enemy) && !isAttackedBy(7,5,enemy) && !isAttackedBy(7,6,enemy))
                out.add(new int[]{7, 6});
            if (whiteQueenSide && board[7][3]=='.' && board[7][2]=='.' && board[7][1]=='.'
                    && !isAttackedBy(7,4,enemy) && !isAttackedBy(7,3,enemy) && !isAttackedBy(7,2,enemy))
                out.add(new int[]{7, 2});
        } else {
            if (blackKingSide  && board[0][5]=='.' && board[0][6]=='.'
                    && !isAttackedBy(0,4,enemy) && !isAttackedBy(0,5,enemy) && !isAttackedBy(0,6,enemy))
                out.add(new int[]{0, 6});
            if (blackQueenSide && board[0][3]=='.' && board[0][2]=='.' && board[0][1]=='.'
                    && !isAttackedBy(0,4,enemy) && !isAttackedBy(0,3,enemy) && !isAttackedBy(0,2,enemy))
                out.add(new int[]{0, 2});
        }
    }

    // ── Check-avoidance filter ────────────────────────────────────────────────

    private boolean wouldLeaveKingInCheck(int fr, int fc, int tr, int tc) {
        char piece = board[fr][fc];
        char type  = Character.toLowerCase(piece);
        boolean w  = Character.isUpperCase(piece);

        // Save
        char sf = board[fr][fc], st = board[tr][tc];
        int rookFromCol = -1, rookToCol = -1;
        char savedRookDst = '.';
        int epCapCol = -1;
        char epCapPiece = '.';

        // Apply
        board[tr][tc] = sf; board[fr][fc] = '.';

        if (type == 'k' && Math.abs(tc - fc) == 2) {
            rookFromCol = (tc > fc) ? 7 : 0;
            rookToCol   = (tc > fc) ? fc + 1 : fc - 1;
            savedRookDst = board[fr][rookToCol];
            board[fr][rookToCol] = board[fr][rookFromCol];
            board[fr][rookFromCol] = '.';
        }
        if (type == 'p' && fc != tc && st == '.') {  // en passant
            epCapCol = tc;
            epCapPiece = board[fr][epCapCol];
            board[fr][epCapCol] = '.';
        }

        boolean inCheck = isInCheck(w);

        // Restore
        board[fr][fc] = sf; board[tr][tc] = st;
        if (rookFromCol >= 0) {
            board[fr][rookFromCol] = board[fr][rookToCol];
            board[fr][rookToCol]   = savedRookDst;
        }
        if (epCapCol >= 0) board[fr][epCapCol] = epCapPiece;

        return inCheck;
    }

    // ── SAN generation ────────────────────────────────────────────────────────

    private String computeSAN(int fr, int fc, int tr, int tc, char promotion) {
        char piece = board[fr][fc];
        char type  = Character.toLowerCase(piece);
        boolean w  = Character.isUpperCase(piece);
        boolean capture = board[tr][tc] != '.' || (type == 'p' && fc != tc && tr == epRow && tc == epCol);
        String toSq = squareName(tr, tc);

        if (type == 'k' && tc - fc ==  2) return "O-O";
        if (type == 'k' && fc - tc ==  2) return "O-O-O";

        if (type == 'p') {
            String s = capture ? (char)('a' + fc) + "x" + toSq : toSq;
            if (tr == 0 || tr == 7) s += "=" + Character.toUpperCase(promotion == 0 ? 'q' : promotion);
            return s;
        }

        String pieceStr = String.valueOf(Character.toUpperCase(type));
        String dis = disambiguation(fr, fc, tr, tc, piece);
        return pieceStr + dis + (capture ? "x" : "") + toSq;
    }

    private String disambiguation(int fr, int fc, int tr, int tc, char piece) {
        List<int[]> others = new ArrayList<>();
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                if ((r != fr || c != fc) && board[r][c] == piece)
                    if (getLegalMoves(r, c).stream().anyMatch(m -> m[0] == tr && m[1] == tc))
                        others.add(new int[]{r, c});
        if (others.isEmpty()) return "";
        if (others.stream().noneMatch(m -> m[1] == fc)) return String.valueOf((char)('a' + fc));
        if (others.stream().noneMatch(m -> m[0] == fr)) return String.valueOf((char)('8' - fr));
        return String.valueOf((char)('a' + fc)) + (8 - fr);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean inBounds(int r, int c) { return r >= 0 && r < 8 && c >= 0 && c < 8; }

    public static String squareName(int row, int col) {
        return String.valueOf((char)('a' + col)) + (8 - row);
    }
}
