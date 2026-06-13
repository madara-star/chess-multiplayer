package com.chess.controller;

import com.chess.engine.MoveResult;
import com.chess.model.Game;
import com.chess.model.GameStateMessage;
import com.chess.model.MoveRequest;
import com.chess.service.GameService;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Optional;

@Controller
public class GameWebSocketController {

    private final GameService gameService;
    private final SimpMessagingTemplate broker;

    public GameWebSocketController(GameService gameService, SimpMessagingTemplate broker) {
        this.gameService = gameService;
        this.broker = broker;
    }

    /**
     * Client sends to: /app/game/{gameId}/move
     * Server broadcasts to: /topic/game/{gameId}
     */
    @MessageMapping("/game/{gameId}/move")
    public void handleMove(@DestinationVariable String gameId, MoveRequest req) {
        Optional<Game> opt = gameService.getGame(gameId);
        if (opt.isEmpty()) {
            broker.convertAndSend("/topic/game/" + gameId,
                    GameStateMessage.error(gameId, "Game not found"));
            return;
        }
        Game game = opt.get();

        if (!game.isFull()) {
            broker.convertAndSend("/topic/game/" + gameId,
                    GameStateMessage.error(gameId, "Waiting for opponent"));
            return;
        }

        MoveResult result = game.makeMove(
            req.getPlayerId(),
            req.getFromRow(), req.getFromCol(),
            req.getToRow(),   req.getToCol(),
            req.getPromotion()
        );

        if (result == null) {
            broker.convertAndSend("/topic/game/" + gameId,
                    GameStateMessage.error(gameId, "Illegal move"));
            return;
        }

        broker.convertAndSend("/topic/game/" + gameId, GameStateMessage.gameState(game));
    }

    /**
     * Client sends to: /app/game/{gameId}/resign
     */
    @MessageMapping("/game/{gameId}/resign")
    public void handleResign(@DestinationVariable String gameId, @Payload String playerId) {
        Optional<Game> opt = gameService.getGame(gameId);
        if (opt.isEmpty()) return;
        Game game = opt.get();
        GameStateMessage msg = GameStateMessage.error(gameId,
            "RESIGN:" + (playerId.equals(game.getWhitePlayerId()) ? "white" : "black"));
        broker.convertAndSend("/topic/game/" + gameId, msg);
    }
}
