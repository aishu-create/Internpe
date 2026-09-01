package com.game.connectfour.model;

public class GameState {
    public static final int ROWS = 6;
    public static final int COLS = 7;
    public static final int EMPTY = 0;
    public static final int PLAYER_1 = 1;
    public static final int PLAYER_2 = 2; // Or System AI

    private int[][] board = new int[ROWS][COLS];
    private int currentPlayer = PLAYER_1;
    private boolean gameOver = false;
    private int winner = EMPTY;

    public int[][] getBoard() { return board; }
    public void setBoard(int[][] board) { this.board = board; }

    public int getCurrentPlayer() { return currentPlayer; }
    public void setCurrentPlayer(int currentPlayer) { this.currentPlayer = currentPlayer; }

    public boolean isGameOver() { return gameOver; }
    public void setGameOver(boolean gameOver) { this.gameOver = gameOver; }

    public int getWinner() { return winner; }
    public void setWinner(int winner) { this.winner = winner; }
}