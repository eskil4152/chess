package com.blikeng.chess.engine.converter;

import com.blikeng.chess.engine.MoveExecutor;
import com.blikeng.chess.engine.PositionMapper;
import com.blikeng.chess.model.Game;
import com.blikeng.chess.model.Move;

public class PgnConverter {
    private PgnConverter() {}

    private static final MoveExecutor moveExecutor = new MoveExecutor();

    public static String toPgn(Game original){
        Game game = new Game(original);

        StringBuilder pgn = new StringBuilder();

        boolean isWhiteTurn = true;
        int moveNumber = 1;

        for (String move : game.getMoves()) {
            Move mappedMove = PositionMapper.fromUci(move);

            String moveSan = SanConverter.toSan(game, mappedMove);

            if (isWhiteTurn) {
                pgn
                        .append(moveNumber++)
                        .append(". ")
                        .append(moveSan)
                        .append(" ");

                isWhiteTurn = false;
            } else {
                pgn.append(moveSan).append(" ");

                isWhiteTurn = true;
            }

            moveExecutor.performMove(game, mappedMove);
        }

        return pgn.toString().trim();
    }
}
