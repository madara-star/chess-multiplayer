# Chess Multiplayer

Real-time multiplayer chess built with **Spring Boot + WebSocket (STOMP)**.  
Friends can join from anywhere with a 6-character game code.

---

## Features
- Full chess rules (castling, en passant, promotion, check/checkmate/stalemate)
- Real-time moves via WebSocket — no polling
- Board flips for black player
- SAN move history, piece animation, check highlighting, confetti on win
- Auto-reconnects if you refresh the page

---

## Run locally

**Prerequisites:** Java 17+, Maven 3.8+

```bash
cd chess-multiplayer
mvn spring-boot:run
```

Open **http://localhost:8080** in two browser tabs (or two machines on the same network).

---

## Build a deployable JAR

```bash
mvn clean package -DskipTests
java -jar target/chess-multiplayer-1.0.0.jar
```

---

## Deploy to Railway (free tier, recommended)

1. Push this folder to a GitHub repo.
2. Go to [railway.app](https://railway.app) → **New Project → Deploy from GitHub Repo**.
3. Select the repo. Railway auto-detects Maven and builds it.
4. Under **Settings → Networking** click **Generate Domain** to get a public URL.
5. Share the URL with friends — they open it and join with the game code.

> Railway passes `PORT` env var automatically; `application.properties` reads it via `${PORT:8080}`.

---

## Deploy to Render (free tier)

1. Push to GitHub.
2. [render.com](https://render.com) → **New Web Service** → connect repo.
3. **Build command:** `mvn clean package -DskipTests`
4. **Start command:** `java -jar target/chess-multiplayer-1.0.0.jar`
5. Choose **Free** plan → **Create Web Service**.

---

## How to play

1. Player 1 opens the app → **Create Game** → shares the 6-char code.
2. Player 2 opens the same URL → **Join Game** → enters the code.
3. Game starts automatically. White moves first.
4. Click a piece to see legal moves (green dots), click a destination to move.

---

## Project structure

```
src/main/java/com/chess/
├── ChessApplication.java          ← Spring Boot entry point
├── config/WebSocketConfig.java    ← STOMP WebSocket config
├── engine/
│   ├── ChessEngine.java           ← Full chess rules engine
│   ├── GameStatus.java
│   └── MoveResult.java
├── model/
│   ├── Game.java                  ← Game state (thread-safe)
│   ├── GameStateMessage.java      ← WebSocket broadcast DTO
│   └── MoveRequest.java           ← Move DTO
├── service/GameService.java       ← In-memory game store
└── controller/
    ├── GameController.java        ← REST endpoints
    └── GameWebSocketController.java ← STOMP handlers

src/main/resources/
├── application.properties
└── static/index.html              ← Full frontend (no build step needed)
```
