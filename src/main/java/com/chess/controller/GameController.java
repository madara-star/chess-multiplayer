package com.chess.controller;

import com.chess.model.Game;
import com.chess.model.GameStateMessage;
import com.chess.service.GameService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/game")
@CrossOrigin(origins = "*")
public class GameController {

    private final GameService gameService;
    private final SimpMessagingTemplate broker;

    public GameController(GameService gameService, SimpMessagingTemplate broker) {
        this.gameService = gameService;
        this.broker = broker;
    }

    /** POST /api/game/create   body: {"playerId":"uuid"} */
    @PostMapping("/create")
    public ResponseEntity<?> createGame(@RequestBody Map<String, String> body) {
        String playerId = body.get("playerId");
        if (playerId == null || playerId.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "playerId required"));
        Game game = gameService.createGame(playerId);
        return ResponseEntity.ok(Map.of(
            "gameId", game.getId(),
            "color",  "white"
        ));
    }

    /** POST /api/game/join/{gameId}   body: {"playerId":"uuid"} */
    @PostMapping("/join/{gameId}")
    public ResponseEntity<?> joinGame(@PathVariable String gameId,
                                      @RequestBody Map<String, String> body) {
        String playerId = body.get("playerId");
        if (playerId == null || playerId.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "playerId required"));
        try {
            Game game = gameService.joinGame(gameId, playerId);
            // Notify both players game has started
            broker.convertAndSend("/topic/game/" + game.getId(), GameStateMessage.gameStart(game));
            return ResponseEntity.ok(Map.of(
                "gameId", game.getId(),
                "color",  "black"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** GET /api/game/{gameId}/legal-moves?playerId=xxx&row=r&col=c */
    @GetMapping("/{gameId}/legal-moves")
    public ResponseEntity<?> legalMoves(@PathVariable String gameId,
                                        @RequestParam String playerId,
                                        @RequestParam int row,
                                        @RequestParam int col) {
        Optional<Game> opt = gameService.getGame(gameId);
        if (opt.isEmpty()) return ResponseEntity.badRequest().body(List.of());
        List<int[]> moves = opt.get().getLegalMoves(playerId, row, col);
        return ResponseEntity.ok(moves);
    }

    /** GET /api/game/{gameId}?playerId=xxx  — for reconnection */
    @GetMapping("/{gameId}")
    public ResponseEntity<?> getGame(@PathVariable String gameId,
                                     @RequestParam(required = false) String playerId) {
        Optional<Game> opt = gameService.getGame(gameId);
        if (opt.isEmpty())
            return ResponseEntity.badRequest().body(Map.of("error", "Game not found"));
        Game game = opt.get();

        String color = "spectator";
        if (playerId != null) {
            if (playerId.equals(game.getWhitePlayerId())) color = "white";
            else if (playerId.equals(game.getBlackPlayerId())) color = "black";
        }

        return ResponseEntity.ok(Map.of(
            "gameId",       game.getId(),
            "color",        color,
            "isFull",       game.isFull(),
            "board",        game.getBoardArray(),
            "whiteToMove",  game.isWhiteToMove(),
            "status",       game.getStatus(),
            "moveHistory",  game.getMoveHistory(),
            "lastMove",     game.getLastMoveSquares() != null ? game.getLastMoveSquares() : new int[]{}
        ));
    }
}
