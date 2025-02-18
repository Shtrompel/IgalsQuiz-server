package com.igalblech.igalsquizserver.network;

import com.corundumstudio.socketio.SocketIOClient;
import com.igalblech.igalsquizserver.Questions.Answer;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public class PlayerHandler
{
    @Getter
    private SocketIOClient webSocket;
    @Getter @Setter
    String uuid;
    @Getter @Setter
    String name;
    @Getter
    int points;
    @Getter @Setter
    Answer answer;


    public PlayerHandler(SocketIOClient webSocket, String uuid) {
        this.webSocket = webSocket;
        this.uuid = uuid;
        this.points = 0;
    }

    public PlayerHandler() {

    }

    public void addPoints(int points) {
        this.points += points;
    }

    public boolean hasAnswer() {
        return answer != null;
    }

    public void reset() {
        points = 0;
        answer = null;
    }
}
