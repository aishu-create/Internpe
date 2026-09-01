package com.game.connectfour.service;

import org.springframework.stereotype.Service;

@Service
public class AiEngine {
    private static final int ROWS = 6;
    private static final int COLS = 7;
    private static final int MAX_DEPTH = 5;

    public int getBestMove(int[][] board, int aiPlayer) {
        int humanPlayer = (aiPlayer == 1) ? 2 : 1;
        int bestScore = Integer.MIN_VALUE;
        int bestCol = 3; // Center preference fallback

        for (int c : new int[]{3, 2, 4, 1, 5, 0, 6}) { // Search from center outwards
            int r = getAvailableRow(board, c);
            if (r != -1) {
                board[r][c] = aiPlayer;
                if (checkWin(board, r, c, aiPlayer)) {
                    board[r][c] = 0;
                    return c; // Immediate win
                }
                int score = minimax(board, MAX_DEPTH - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false, aiPlayer, humanPlayer);
                board[r][c] = 0;

                if (score > bestScore) {
                    bestScore = score;
                    bestCol = c;
                }
            }
        }
        return bestCol;
    }

    private int minimax(int[][] board, int depth, int alpha, int beta, boolean isMaximizing, int aiPlayer, int humanPlayer) {
        if (depth == 0) return evaluateBoard(board, aiPlayer);

        if (isMaximizing) {
            int maxScore = Integer.MIN_VALUE;
            for (int c = 0; c < COLS; c++) {
                int r = getAvailableRow(board, c);
                if (r != -1) {
                    board[r][c] = aiPlayer;
                    if (checkWin(board, r, c, aiPlayer)) {
                        board[r][c] = 0;
                        return 100000 + depth;
                    }
                    int score = minimax(board, depth - 1, alpha, beta, false, aiPlayer, humanPlayer);
                    board[r][c] = 0;
                    maxScore = Math.max(maxScore, score);
                    alpha = Math.max(alpha, score);
                    if (beta <= alpha) break;
                }
            }
            return maxScore;
        } else {
            int minScore = Integer.MAX_VALUE;
            for (int c = 0; c < COLS; c++) {
                int r = getAvailableRow(board, c);
                if (r != -1) {
                    board[r][c] = humanPlayer;
                    if (checkWin(board, r, c, humanPlayer)) {
                        board[r][c] = 0;
                        return -100000 - depth;
                    }
                    int score = minimax(board, depth - 1, alpha, beta, true, aiPlayer, humanPlayer);
                    board[r][c] = 0;
                    minScore = Math.min(minScore, score);
                    beta = Math.min(beta, score);
                    if (beta <= alpha) break;
                }
            }
            return minScore;
        }
    }

    private int evaluateBoard(int[][] board, int player) {
        int score = 0;
        int centerCol = COLS / 2;
        int centerCount = 0;
        for (int r = 0; r < ROWS; r++) {
            if (board[r][centerCol] == player) centerCount++;
        }
        score += centerCount * 6;
        return score;
    }

    private int getAvailableRow(int[][] board, int col) {
        for (int r = ROWS - 1; r >= 0; r--) {
            if (board[r][col] == 0) return r;
        }
        return -1;
    }

    public boolean checkWin(int[][] board, int row, int col, int player) {
        int[][] directions = {{0,1}, {1,0}, {1,1}, {1,-1}};
        for (int[] d : directions) {
            int count = 1;
            for (int i = 1; i <= 3; i++) {
                int r = row + d[0] * i, c = col + d[1] * i;
                if (r >= 0 && r < ROWS && c >= 0 && c < COLS && board[r][c] == player) count++;
                else break;
            }
            for (int i = 1; i <= 3; i++) {
                int r = row - d[0] * i, c = col - d[1] * i;
                if (r >= 0 && r < ROWS && c >= 0 && c < COLS && board[r][c] == player) count++;
                else break;
            }
            if (count >= 4) return true;
        }
        return false;
    }
}
    

