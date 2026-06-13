package com.chess.service;

import com.chess.model.Game;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameService {

    private final Map<String, Game> games = new ConcurrentHashMap<>();

    /** Create a new game, returning the Game (creator plays white). */
    public Game createGame(String playerId) {
        String gameId = generateId();
        Game game = new Game(gameId, playerId);
        games.put(gameId, game);
        return game;
    }

    /**
     * Join an existing game as black.
     * Throws IllegalArgumentException if not found or already full.
     */
    public Game joinGame(String gameId, String playerId) {
        Game game = games.get(gameId.toUpperCase());
        if (game == null) throw new IllegalArgumentException("Game not found: " + gameId);
        if (game.isFull()) throw new IllegalStateException("Game is already full");
        if (playerId.equals(game.getWhitePlayerId()))
            throw new IllegalStateException("You are already in this game as white");
        game.setBlackPlayerId(playerId);
        return game;
    }

    public Optional<Game> getGame(String gameId) {
        return Optional.ofNullable(games.get(gameId.toUpperCase()));
    }

    /** Remove stale games (simple cleanup). */
    public void removeGame(String gameId) {
        games.remove(gameId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String generateId() {
        // 6-char uppercase alphanumeric
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        Random rng = new Random();
        String id;
        do {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) sb.append(chars.charAt(rng.nextInt(chars.length())));
            id = sb.toString();
        } while (games.containsKey(id));
        return id;
    }
}
