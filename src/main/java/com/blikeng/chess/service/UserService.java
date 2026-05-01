package com.blikeng.chess.service;

import com.blikeng.chess.dto.GamePreviewDTO;
import com.blikeng.chess.model.GameStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    public String getSelf(){
        return "";
    }

    public String getUser(String username){
        return "";
    }

    public List<GamePreviewDTO> getGames(String username){
        return List.of(new GamePreviewDTO(null, null, null, GameStatus.DRAW));
    }
}
