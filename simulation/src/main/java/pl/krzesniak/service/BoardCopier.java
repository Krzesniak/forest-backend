package pl.krzesniak.service;


import pl.krzesniak.model.ForestPixel;

public class BoardCopier {

    public static ForestPixel[][] createCopyOfBoard(ForestPixel[][] board) {
        ForestPixel[][] newBoard = new ForestPixel[board.length][board[0].length];
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[0].length; j++) {
                newBoard[i][j] = board[i][j].createCopy();
            }
        }
        return newBoard;
    }
}
