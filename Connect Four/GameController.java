package com.game.connectfour.controller;

import com.game.connectfour.service.AiEngine;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/game")
@CrossOrigin(origins = "*")
public class GameController {

    private final AiEngine aiEngine;

    public GameController(AiEngine aiEngine) {
        this.aiEngine = aiEngine;
    }

    @PostMapping("/ai-move")
    public Map<String, Integer> computeAiMove(@RequestBody Map<String, Object> payload) {
        var rawBoard = (java.util.List<java.util.List<Integer>>) payload.get("board");
        int aiPlayer = (int) payload.getOrDefault("aiPlayer", 2);

        int[][] board = new int[6][7];
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 7; j++) {
                board[i][j] = rawBoard.get(i).get(j);
            }
        }

        int col = aiEngine.getBestMove(board, aiPlayer);
        return Map.of("column", col);
    }
}
