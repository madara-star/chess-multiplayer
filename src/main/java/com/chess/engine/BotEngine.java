package com.chess.engine;

import java.util.*;

/**
 * Self-contained chess AI engine for bot play and move analysis.
 * Implements minimax with alpha-beta pruning + piece-square tables.
 */
public class BotEngine {

    // ── Piece values ──────────────────────────────────────────────────────────
    private static final Map<Character, Integer> PV = new HashMap<>();
    static {
        PV.put('P', 100);  PV.put('N', 320);  PV.put('B', 330);
        PV.put('R', 500);  PV.put('Q', 900);  PV.put('K', 20000);
        PV.put('p', 100);  PV.put('n', 320);  PV.put('b', 330);
        PV.put('r', 500);  PV.put('q', 900);  PV.put('k', 20000);
    }

    // ── Piece-Square Tables ───────────────────────────────────────────────────
    private static final int[] PST_P = {
        0,0,0,0,0,0,0,0, 50,50,50,50,50,50,50,50, 10,10,20,30,30,20,10,10,
        5,5,10,25,25,10,5,5, 0,0,0,20,20,0,0,0, 5,-5,-10,0,0,-10,-5,5,
        5,10,10,-20,-20,10,10,5, 0,0,0,0,0,0,0,0
    };
    private static final int[] PST_N = {
        -50,-40,-30,-30,-30,-30,-40,-50, -40,-20,0,0,0,0,-20,-40,
        -30,0,10,15,15,10,0,-30, -30,5,15,20,20,15,5,-30,
        -30,0,15,20,20,15,0,-30, -30,5,10,15,15,10,5,-30,
        -40,-20,0,5,5,0,-20,-40, -50,-40,-30,-30,-30,-30,-40,-50
    };
    private static final int[] PST_B = {
        -20,-10,-10,-10,-10,-10,-10,-20, -10,0,0,0,0,0,0,-10,
        -10,0,5,10,10,5,0,-10, -10,5,5,10,10,5,5,-10,
        -10,0,10,10,10,10,0,-10, -10,10,10,10,10,10,10,-10,
        -10,5,0,0,0,0,5,-10, -20,-10,-10,-10,-10,-10,-10,-20
    };
    private static final int[] PST_R = {
        0,0,0,0,0,0,0,0, 5,10,10,10,10,10,10,5, -5,0,0,0,0,0,0,-5,
        -5,0,0,0,0,0,0,-5, -5,0,0,0,0,0,0,-5, -5,0,0,0,0,0,0,-5,
        -5,0,0,0,0,0,0,-5, 0,0,0,5,5,0,0,0
    };
    private static final int[] PST_Q = {
        -20,-10,-10,-5,-5,-10,-10,-20, -10,0,0,0,0,0,0,-10,
        -10,0,5,5,5,5,0,-10, -5,0,5,5,5,5,0,-5,
        0,0,5,5,5,5,0,-5, -10,5,5,5,5,5,0,-10,
        -10,0,5,0,0,0,0,-10, -20,-10,-10,-5,-5,-10,-10,-20
    };
    private static final int[] PST_K = {
        -30,-40,-40,-50,-50,-40,-40,-30, -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30, -30,-40,-40,-50,-50,-40,-40,-30,
        -20,-30,-30,-40,-40,-30,-30,-20, -10,-20,-20,-20,-20,-20,-20,-10,
        20,20,0,0,0,0,20,20, 20,30,10,0,0,10,30,20
    };

    // ── State ─────────────────────────────────────────────────────────────────
    public static class State {
        public boolean wt, wKS, wQS, bKS, bQS;
        public int epR, epC;

        public State(boolean wt, boolean wKS, boolean wQS, boolean bKS, boolean bQS, int epR, int epC) {
            this.wt = wt; this.wKS = wKS; this.wQS = wQS;
            this.bKS = bKS; this.bQS = bQS; this.epR = epR; this.epC = epC;
        }

        public State copy() {
            return new State(wt, wKS, wQS, bKS, bQS, epR, epC);
        }
    }

    public static class MoveApplied {
        public final char[][] board;
        public final State state;
        public MoveApplied(char[][] board, State state) { this.board = board; this.state = state; }
    }

    public static class BestMove {
        public final int fromRow, fromCol, toRow, toCol, eval;
        public BestMove(int fr, int fc, int tr, int tc, int eval) {
            this.fromRow = fr; this.fromCol = fc; this.toRow = tr; this.toCol = tc; this.eval = eval;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private static boolean inBounds(int r, int c) { return r >= 0 && r < 8 && c >= 0 && c < 8; }
    private static boolean isWhite(char p)  { return p != '.' && Character.isUpperCase(p); }
    private static boolean isEmpty(char p)  { return p == '.'; }

    private static char[][] cloneBoard(char[][] b) {
        char[][] c = new char[8][8];
        for (int i = 0; i < 8; i++) c[i] = b[i].clone();
        return c;
    }

    private static boolean pathClear(char[][] b, int fr, int fc, int tr, int tc) {
        int dr = Integer.signum(tr - fr), dc = Integer.signum(tc - fc);
        int r = fr + dr, c = fc + dc;
        while (r != tr || c != tc) {
            if (b[r][c] != '.') return false;
            r += dr; c += dc;
        }
        return true;
    }

    private static boolean attacks(char[][] b, int fr, int fc, int tr, int tc) {
        char p = b[fr][fc];
        if (p == '.') return false;
        boolean w = isWhite(p);
        int dr = tr - fr, dc = tc - fc;
        switch (Character.toLowerCase(p)) {
            case 'p': return dr == (w ? -1 : 1) && Math.abs(dc) == 1;
            case 'n': return (Math.abs(dr)==2&&Math.abs(dc)==1)||(Math.abs(dr)==1&&Math.abs(dc)==2);
            case 'k': return Math.abs(dr)<=1 && Math.abs(dc)<=1 && (dr!=0||dc!=0);
            case 'b': return Math.abs(dr)==Math.abs(dc) && dr!=0 && pathClear(b,fr,fc,tr,tc);
            case 'r': return (dr==0||dc==0) && (dr!=0||dc!=0) && pathClear(b,fr,fc,tr,tc);
            case 'q': return ((dr==0||dc==0)||(Math.abs(dr)==Math.abs(dc))) && (dr!=0||dc!=0) && pathClear(b,fr,fc,tr,tc);
        }
        return false;
    }

    private static boolean squareAttackedBy(char[][] b, int row, int col, boolean byWhite) {
        for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++) {
            if (b[r][c] == '.') continue;
            if (isWhite(b[r][c]) != byWhite) continue;
            if (attacks(b, r, c, row, col)) return true;
        }
        return false;
    }

    private static boolean inCheck(char[][] b, boolean white) {
        char king = white ? 'K' : 'k';
        for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++)
            if (b[r][c] == king) return squareAttackedBy(b, r, c, !white);
        return false;
    }

    // ── Move generation ───────────────────────────────────────────────────────
    private static List<int[]> pseudoMoves(char[][] b, State s, int row, int col) {
        List<int[]> mv = new ArrayList<>();
        char p = b[row][col];
        if (p == '.') return mv;
        boolean w = isWhite(p);

        switch (Character.toLowerCase(p)) {
            case 'p': {
                int d = w ? -1 : 1, sr = w ? 6 : 1;
                if (inBounds(row+d,col) && b[row+d][col]=='.') {
                    mv.add(new int[]{row+d,col});
                    if (row==sr && b[row+2*d][col]=='.') mv.add(new int[]{row+2*d,col});
                }
                for (int dc : new int[]{-1,1}) {
                    int nr=row+d, nc=col+dc;
                    if (!inBounds(nr,nc)) continue;
                    if (b[nr][nc]!='.' && isWhite(b[nr][nc])!=w) mv.add(new int[]{nr,nc});
                    if (nr==s.epR && nc==s.epC) mv.add(new int[]{nr,nc});
                }
                break;
            }
            case 'n': {
                for (int[] d : new int[][]{{-2,-1},{-2,1},{-1,-2},{-1,2},{1,-2},{1,2},{2,-1},{2,1}}) {
                    int nr=row+d[0], nc=col+d[1];
                    if (inBounds(nr,nc) && (b[nr][nc]=='.'||isWhite(b[nr][nc])!=w)) mv.add(new int[]{nr,nc});
                }
                break;
            }
            case 'b': case 'r': case 'q': {
                int[][] dirs;
                char t = Character.toLowerCase(p);
                if (t=='b')      dirs = new int[][]{{1,1},{1,-1},{-1,1},{-1,-1}};
                else if (t=='r') dirs = new int[][]{{1,0},{-1,0},{0,1},{0,-1}};
                else             dirs = new int[][]{{1,1},{1,-1},{-1,1},{-1,-1},{1,0},{-1,0},{0,1},{0,-1}};
                for (int[] dir : dirs) {
                    int nr=row+dir[0], nc=col+dir[1];
                    while (inBounds(nr,nc)) {
                        if (b[nr][nc]=='.') mv.add(new int[]{nr,nc});
                        else { if (isWhite(b[nr][nc])!=w) mv.add(new int[]{nr,nc}); break; }
                        nr+=dir[0]; nc+=dir[1];
                    }
                }
                break;
            }
            case 'k': {
                for (int[] d : new int[][]{{-1,-1},{-1,0},{-1,1},{0,-1},{0,1},{1,-1},{1,0},{1,1}}) {
                    int nr=row+d[0], nc=col+d[1];
                    if (inBounds(nr,nc) && (b[nr][nc]=='.'||isWhite(b[nr][nc])!=w)) mv.add(new int[]{nr,nc});
                }
                boolean eny = !w;
                if (w) {
                    if (s.wKS && b[7][5]=='.'&&b[7][6]=='.'&&!squareAttackedBy(b,7,4,eny)&&!squareAttackedBy(b,7,5,eny)&&!squareAttackedBy(b,7,6,eny)) mv.add(new int[]{7,6});
                    if (s.wQS && b[7][3]=='.'&&b[7][2]=='.'&&b[7][1]=='.'&&!squareAttackedBy(b,7,4,eny)&&!squareAttackedBy(b,7,3,eny)&&!squareAttackedBy(b,7,2,eny)) mv.add(new int[]{7,2});
                } else {
                    if (s.bKS && b[0][5]=='.'&&b[0][6]=='.'&&!squareAttackedBy(b,0,4,eny)&&!squareAttackedBy(b,0,5,eny)&&!squareAttackedBy(b,0,6,eny)) mv.add(new int[]{0,6});
                    if (s.bQS && b[0][3]=='.'&&b[0][2]=='.'&&b[0][1]=='.'&&!squareAttackedBy(b,0,4,eny)&&!squareAttackedBy(b,0,3,eny)&&!squareAttackedBy(b,0,2,eny)) mv.add(new int[]{0,2});
                }
                break;
            }
        }
        return mv;
    }

    public static MoveApplied applyMove(char[][] b, State s, int fr, int fc, int tr, int tc, char promo) {
        char[][] b2 = cloneBoard(b);
        State s2 = s.copy();
        char p = b2[fr][fc];
        boolean w = isWhite(p);
        char t = Character.toLowerCase(p);

        if (t=='p' && fc!=tc && b2[tr][tc]=='.') b2[fr][tc] = '.'; // en passant
        b2[tr][tc] = b2[fr][fc];
        b2[fr][fc] = '.';
        if (t=='k' && Math.abs(tc-fc)==2) { int rf=tc>fc?7:0, rt=tc>fc?fc+1:fc-1; b2[fr][rt]=b2[fr][rf]; b2[fr][rf]='.'; }
        if (t=='k') { if(w){s2.wKS=false;s2.wQS=false;}else{s2.bKS=false;s2.bQS=false;} }
        if (t=='r') { if(w){if(fc==7)s2.wKS=false;if(fc==0)s2.wQS=false;}else{if(fc==7)s2.bKS=false;if(fc==0)s2.bQS=false;} }
        if(tr==7&&tc==7)s2.wKS=false; if(tr==7&&tc==0)s2.wQS=false;
        if(tr==0&&tc==7)s2.bKS=false; if(tr==0&&tc==0)s2.bQS=false;
        if (t=='p' && (tr==0||tr==7)) b2[tr][tc] = w ? Character.toUpperCase(promo) : promo;
        s2.epR = -1; s2.epC = -1;
        if (t=='p' && Math.abs(tr-fr)==2) { s2.epR=(fr+tr)/2; s2.epC=fc; }
        s2.wt = !s2.wt;
        return new MoveApplied(b2, s2);
    }

    private static List<int[]> getLegalMoves(char[][] b, State s, int row, int col) {
        List<int[]> legal = new ArrayList<>();
        for (int[] m : pseudoMoves(b, s, row, col)) {
            MoveApplied r = applyMove(b, s, row, col, m[0], m[1], 'q');
            if (!inCheck(r.board, isWhite(b[row][col]))) legal.add(m);
        }
        return legal;
    }

    public static List<int[]> getAllLegalMoves(char[][] b, State s) {
        List<int[]> all = new ArrayList<>();
        for (int r=0; r<8; r++) for (int c=0; c<8; c++) {
            if (b[r][c]=='.' || isWhite(b[r][c])!=s.wt) continue;
            for (int[] m : getLegalMoves(b, s, r, c)) all.add(new int[]{r,c,m[0],m[1]});
        }
        return all;
    }

    // ── Evaluation ────────────────────────────────────────────────────────────
    private static int pst(char p, int r, int c) {
        boolean w = isWhite(p);
        int i = w ? r*8+c : (7-r)*8+c;
        switch (Character.toLowerCase(p)) {
            case 'p': return PST_P[i]; case 'n': return PST_N[i];
            case 'b': return PST_B[i]; case 'r': return PST_R[i];
            case 'q': return PST_Q[i]; case 'k': return PST_K[i];
        }
        return 0;
    }

    public static int evaluate(char[][] b) {
        int score = 0;
        for (int r=0; r<8; r++) for (int c=0; c<8; c++) {
            char p = b[r][c];
            if (p=='.') continue;
            int v = PV.getOrDefault(p, 0) + pst(p, r, c);
            score += isWhite(p) ? v : -v;
        }
        return score;
    }

    // ── Minimax with alpha-beta ────────────────────────────────────────────────
    private static int minimax(char[][] b, State s, int depth, int alpha, int beta, boolean maximizing) {
        if (depth == 0) return evaluate(b);
        List<int[]> moves = getAllLegalMoves(b, s);
        if (moves.isEmpty()) return inCheck(b, s.wt) ? (maximizing ? -999999 : 999999) : 0;
        if (maximizing) {
            int best = Integer.MIN_VALUE;
            for (int[] m : moves) {
                MoveApplied r = applyMove(b, s, m[0], m[1], m[2], m[3], 'q');
                best = Math.max(best, minimax(r.board, r.state, depth-1, alpha, beta, false));
                alpha = Math.max(alpha, best);
                if (beta <= alpha) break;
            }
            return best;
        } else {
            int best = Integer.MAX_VALUE;
            for (int[] m : moves) {
                MoveApplied r = applyMove(b, s, m[0], m[1], m[2], m[3], 'q');
                best = Math.min(best, minimax(r.board, r.state, depth-1, alpha, beta, true));
                beta = Math.min(beta, best);
                if (beta <= alpha) break;
            }
            return best;
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Find best move.
     * Easy  = random legal move
     * Medium = depth 2 minimax
     * Hard   = depth 4 minimax
     */
    public static BestMove bestMove(char[][] b, State s, String difficulty) {
        List<int[]> moves = getAllLegalMoves(b, s);
        if (moves.isEmpty()) return null;

        if ("easy".equals(difficulty)) {
            int[] m = moves.get(new Random().nextInt(moves.size()));
            return new BestMove(m[0], m[1], m[2], m[3], evaluate(b));
        }

        int depth = "hard".equals(difficulty) ? 4 : 2;
        boolean max = s.wt;
        int bestSc = max ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        int[] bestMv = moves.get(0);

        for (int[] m : moves) {
            MoveApplied r = applyMove(b, s, m[0], m[1], m[2], m[3], 'q');
            int sc = minimax(r.board, r.state, depth-1, Integer.MIN_VALUE, Integer.MAX_VALUE, !max);
            if (max ? sc > bestSc : sc < bestSc) { bestSc = sc; bestMv = m; }
        }
        return new BestMove(bestMv[0], bestMv[1], bestMv[2], bestMv[3], bestSc);
    }

    /**
     * Find best move and its evaluation at a fixed depth (used for analysis).
     */
    public static BestMove bestMoveAtDepth(char[][] b, State s, int depth) {
        List<int[]> moves = getAllLegalMoves(b, s);
        if (moves.isEmpty()) return null;
        boolean max = s.wt;
        int bestSc = max ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        int[] bestMv = moves.get(0);
        for (int[] m : moves) {
            MoveApplied r = applyMove(b, s, m[0], m[1], m[2], m[3], 'q');
            int sc = minimax(r.board, r.state, depth-1, Integer.MIN_VALUE, Integer.MAX_VALUE, !max);
            if (max ? sc > bestSc : sc < bestSc) { bestSc = sc; bestMv = m; }
        }
        return new BestMove(bestMv[0], bestMv[1], bestMv[2], bestMv[3], bestSc);
    }
}
