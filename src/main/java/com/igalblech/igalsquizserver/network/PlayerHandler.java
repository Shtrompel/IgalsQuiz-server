package com.igalblech.igalsquizserver.network;

import com.corundumstudio.socketio.SocketIOClient;
import com.igalblech.igalsquizserver.Questions.Answer;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@ToString
public class PlayerHandler
{
    @Getter @Setter
    private SocketIOClient webSocket;
    @Getter @Setter
    String uuid;
    @Getter @Setter
    String name;
    @Getter
    int points;

    private Answer answer;
    @Setter
    boolean active = true;


    public PlayerHandler(SocketIOClient webSocket, String uuid) {
        this.webSocket = webSocket;
        this.uuid = uuid;
        this.points = 0;
        this.active = true;
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

    public boolean isActive() {
        return active;
    }

    public Answer getAnswer() {
        return answer;
    }

    public void setAnswer(Answer answer) {
        this.answer = answer;
    }
}
