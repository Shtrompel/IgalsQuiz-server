package com.igalblech.igalsquizserver.controllers;

import com.igalblech.igalsquizserver.InterfaceController;
import com.igalblech.igalsquizserver.QuizApplication;
import com.igalblech.igalsquizserver.SharedSessionData;
import com.igalblech.igalsquizserver.network.PlayerHandler;
import com.igalblech.igalsquizserver.network.QuizServerSocket;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebErrorEvent;
import javafx.scene.web.WebView;
import javafx.scene.input.KeyEvent;

import java.net.URL;
import java.util.*;

public class QuizEndController implements InterfaceController {

    @FXML
    VBox boxScreen;

    @FXML
    Button btnToMenu;

    private Application app;
    private SceneManager sceneManager;

    @FXML
    private WebView webView;

    public void initialize()
    {
    }

    public void btnToMenuPressed()
    {
        sceneManager.changeScene("MENU");
    }


    @Override
    public void onShown() {
        SharedSessionData userData = (SharedSessionData) sceneManager.getUserData();
        QuizServerSocket socket = userData.getServerSocket();
        socket.sendEndGame(false);

        WebEngine webEngine = webView.getEngine();

        webEngine.setOnError(event ->
                System.err.println("WebEngine Error: " + event.getMessage()));

        // Optional: Listen to document changes
        webEngine.documentProperty().addListener((observable, oldDoc, newDoc) -> {
            if (newDoc != null) {
                PlayerHandler[] top = getTop3();
                String s = String.format(
                        "updatePodium(['%s', '%s', '%s'], ['%d', '%d', '%d'])",
                        getSafeName(top[1]), getSafeName(top[0]), getSafeName(top[2]),
                        getSafePoints(top[1]), getSafePoints(top[0]), getSafePoints(top[2])
                );
                webEngine.executeScript(s);
            }
        });

        URL url = QuizApplication.class.getResource("podium.html");
        System.out.println(url.toExternalForm());
        if (url != null) {
            webEngine.load(url.toExternalForm());
        } else {
            System.out.println("Error: podium.html not found!");
            webEngine.loadContent("<html><body><h1>podium.html not found!</h1></body></html>");
        }

    }

    private String getSafeName(PlayerHandler player) {
        return (player != null) ? player.getName() : "";
    }

    private int getSafePoints(PlayerHandler player) {
        return (player != null) ? player.getPoints() : 0;
    }


    PlayerHandler[] getTop3()
    {
        SharedSessionData userData = (SharedSessionData) sceneManager.getUserData();
        Map<String, PlayerHandler> players = userData.getServerSocket().getPlayerHandlers();

        NavigableMap<Integer, PlayerHandler> playersRanked = new TreeMap<>(Comparator.reverseOrder());
        for (PlayerHandler p : players.values())
        {
            playersRanked.put(p.getPoints(), p);
        }
        Iterator<Map.Entry<Integer, PlayerHandler>> iterator = playersRanked.entrySet().iterator();

        PlayerHandler[] out = new PlayerHandler[]{null, null, null};
        if (iterator.hasNext()) out[0] = iterator.next().getValue();
        if (iterator.hasNext()) out[1] = iterator.next().getValue();
        if (iterator.hasNext()) out[2] = iterator.next().getValue();
        return out;
    }

    @Override
    public void setScene(SceneManager stage) {
        this.sceneManager = stage;
    }

    @Override
    public void setApp(Application app) {
        this.app = app;
    }

    @Override
    public void onClose() {
    }

    @Override
    public void onKeyboardButtonPressed(KeyEvent e) {
    }

    @Override
    public void onSendQuestionEnd() {

    }

}
