package com.igalblech.igalsquizserver.controllers;

import com.igalblech.igalsquizserver.Questions.Answer;
import com.igalblech.igalsquizserver.QuizApplication;
import com.igalblech.igalsquizserver.SharedSessionData;
import com.igalblech.igalsquizserver.network.PlayerHandler;
import com.igalblech.igalsquizserver.ui.BarChartExt;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;


public class QuestionTransitionController  implements InterfaceController {

    public static class Player {
        private String name;
        private int points;

        public Player(String name, int points) {
            this.name = name;
            this.points = points;
        }

        public String getName() {
            return name;
        }

        public int getPoints() {
            return points;
        }
    }

    SceneManager stageManager;
    @FXML
    Pane paneWithGraph;
    @FXML
    VBox vbox;
    @FXML
    Text textCountdownTimer;
    @FXML
    Text textAnswersInfo;

    BarChartExt barChart;

    int waitTime = 30;
    int timeStart;

    Timeline countdown;

    WebView webView;

    public void initialize() {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Devices");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Visits");


        this.webView = new WebView();
        paneWithGraph.getChildren().add(webView);
    }

    public void onButtonNextQuestionPressed() {
        nextStage();
    }

    @Override
    public void setScene(SceneManager stageManager) {
        this.stageManager = stageManager;
    }

    @Override
    public void setApp(Application app) {
    }

    @Override
    public void onShown() {

        ((SharedSessionData)this.stageManager.getUserData()).playPostQuestion();

        timeStart = waitTime;
        countdown = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> {
                    timeStart -= 1;
                    textCountdownTimer.setVisible(true);
                    textCountdownTimer.setText(String.valueOf(timeStart));
                })
        );
        countdown.setCycleCount(waitTime);
        countdown.setOnFinished(event -> onCountDownFinished());
        countdown.play();
        textCountdownTimer.setText(String.valueOf(timeStart));

        URI path = null;
        try {
            path = QuizApplication.getFileURL("ranking_chart.html").toURI();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        webView.getEngine().load(path.toString());
        webView.getEngine().setJavaScriptEnabled(true);
        webView.getEngine().setOnAlert(event -> System.out.println("Alert: " + event.getData()));

        Collection<PlayerHandler> players;
        players = ((SharedSessionData) stageManager.getUserData()).getServerSocket().getPlayerHandlers().values();

        for (PlayerHandler player : players)
        {
            if (player.getAnswer() == null) {
                throw new IllegalStateException("Player answer should not be null");
            }
        }

        /*
        int x = 0;
        for (int i = 0 ; i < 7; i++)
        {
            PlayerHandler playerHandler;
            x += (int) (Math.random()*100);
            playerHandler = new PlayerHandler();
            playerHandler.setName("player" + i);
            playerHandler.setUuid("player" + i);
            playerHandler.addPoints(x);
            Answer answer = new Answer();
            answer.setValidity(Math.random() > 0.5 ? Answer.Validity.WRONG : (Math.random() > 0.5 ? Answer.Validity.PARTIALLY_RIGHT : Answer.Validity.RIGHT));
            playerHandler. setAnswer(answer);
            players.add(playerHandler);
        }

         */


        int rightCount = 0, okayCount = 0, wrongCount = 0;

        for (PlayerHandler player : players)
        {

            switch (player.getAnswer().getValidity())
            {
                case RIGHT:
                    rightCount++;
                    break;
                case PARTIALLY_RIGHT:
                    okayCount++;
                    break;
                case WRONG:
                    wrongCount++;
                    break;
            }
        }
        String answersInfoStr = "";
        if (rightCount != 0)
            answersInfoStr += rightCount + " people were right\n";
        if (okayCount != 0)
            answersInfoStr += okayCount + " people were half right\n";
        if (wrongCount != 0)
            answersInfoStr += wrongCount + " people were wrong\n";
        textAnswersInfo.setText(answersInfoStr);


        PlayerHandler[] sortedPlayers = players.toArray(new PlayerHandler[0]);
        Arrays.sort(sortedPlayers, (o1, o2) -> Integer.compare(o2.getPoints(), o1.getPoints()));

        int maxValue = (sortedPlayers.length > 0) ? sortedPlayers[0].getPoints() : 0;
        JSONArray playersJson = new JSONArray();
        for (int i = 0; i < Math.min(10, sortedPlayers.length); i++)
        {
            if (i >= sortedPlayers.length)
                continue;
            PlayerHandler player = sortedPlayers[i];
            JSONObject playerJson = new JSONObject();
            playerJson.put("name", player.getName());
            playerJson.put("points", player.getPoints());

            String status = "";
            switch (player.getAnswer().getValidity())
            {
                case RIGHT:
                    status = "✔";
                    rightCount++;
                    break;
                case PARTIALLY_RIGHT:
                    status = "-✔";
                    okayCount++;
                    break;
                case WRONG:
                    status = "✖";
                    wrongCount++;
                    break;
            }
            playerJson.put("status", status);

            playersJson.put(playerJson);
        }
        String playersJsonString = playersJson.toString();

        // Pass JSON data to JavaScript
        webView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                webView.getEngine().executeScript("updateChart(" + playersJsonString + ", " + maxValue + ")");
            }
        });

        for (PlayerHandler playerHandler : players)
        {
            playerHandler.setAnswer(null);
        }
    }

    private void testPlayers() {
        for (int i = 0; i < 3; i++)
        {
            PlayerHandler p = new PlayerHandler();
            p.setName("name" + (int)(Math.random()*100));
            p.setUuid(p.getName());
            p.addPoints((int) (Math.random() * 200));
            ((SharedSessionData) stageManager.getUserData()).getServerSocket().getPlayerHandlers().put(
                    p.getUuid(), p
            );
        }
        Collection<PlayerHandler> players;
        players = ((SharedSessionData) stageManager.getUserData()).getServerSocket().getPlayerHandlers().values();
        for (PlayerHandler p : players)
        {
            p.addPoints((int) (Math.random() * 400));
        }
    }

    @Override
    public void onClose() {
        if (countdown != null)
            countdown.stop();
        countdown = null;
    }

    @Override
    public void onKeyboardButtonPressed(KeyEvent e) {

    }

    @Override
    public void onSendQuestionEnd() {

    }

    private void onCountDownFinished() {
        nextStage();
    }

    private void nextStage()
    {
        countdown.stop();

        stageManager.changeScene("QUESTION_INTRO");
    }
}
