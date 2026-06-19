package com.blikeng.chess.engine.converter;

import com.blikeng.chess.engine.MoveExecutor;
import com.blikeng.chess.engine.PositionMapper;
import com.blikeng.chess.model.Game;
import com.blikeng.chess.model.Move;

/**
 * Exports a finished {@link Game} to PGN movetext, e.g. {@code 1. e4 e5 2. Nf3 Nc6}.
 *
 * <p>Outputs the movetext <em>only</em>. No tag-pair header ({@code [Event]},
 * {@code [White]}, {@code [Result]}, …) and no trailing result token.
 *
 * <p>Replays the game's recorded moves on a fresh copy, so the original game is not
 * mutated during export.
 */
public class PgnConverter {
    private PgnConverter() {}

    private static final MoveExecutor moveExecutor = new MoveExecutor();

    public static String toPgn(Game original){
        Game game = new Game(
            original.getId(),
            original.getWhiteId(),
            original.getWhiteUsername(),
            original.getBlackId(),
            original.getBlackUsername(),
            original.getWhiteElo(),
            original.getBlackElo(),
            original.getTimeControl(),
            original.getWhiteRemainingMs(),
            original.getBlackRemainingMs(),
            original.getTurnStartTime()
        );

        StringBuilder pgn = new StringBuilder();

        boolean isWhiteTurn = true;
        int moveNumber = 1;

        for (String move : original.getMoves()) {
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
