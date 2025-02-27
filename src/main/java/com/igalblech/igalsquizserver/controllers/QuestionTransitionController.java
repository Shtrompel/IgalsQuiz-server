package com.igalblech.igalsquizserver.controllers;

import com.igalblech.igalsquizserver.InterfaceController;
import com.igalblech.igalsquizserver.SharedSessionData;
import com.igalblech.igalsquizserver.network.PlayerHandler;
import com.igalblech.igalsquizserver.ui.BarChartExt;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.*;


public class QuestionTransitionController  implements InterfaceController {

    SceneManager stageManager;
    @FXML
    Pane paneWithGraph;
    @FXML
    VBox vbox;
    @FXML
    Text textCountdownTimer;

    BarChartExt barChart;

    int waitTime = 30;
    int timeStart;

    Timeline countdown;

    public void initialize() {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Devices");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Visits");

        this.barChart = new BarChartExt(xAxis, yAxis);

            barChart.getData().clear();
            barChart.setLegendVisible(false);
            barChart.setVerticalGridLinesVisible(false);
            barChart.setVerticalGridLinesVisible(false);
            barChart.setLegendVisible(false);
            barChart.setHorizontalGridLinesVisible(false);
            barChart.setHorizontalZeroLineVisible(false);
            barChart.setAlternativeColumnFillVisible(false);
            barChart.setAlternativeRowFillVisible(false);

            Axis axis;
            axis = barChart.getXAxis();
            axis.setVisible(false);
            axis.setTickLabelsVisible(true);
            axis.setTickMarkVisible(false);
            axis.setLabel("");

            axis = barChart.getYAxis();
            axis.setVisible(false);
            axis.setTickLabelsVisible(false);
            axis.setTickMarkVisible(false);
            axis.setOpacity(0);

        barChart.setStyle(//".axis .tick-label {\n" +
                "    -fx-font-size: 32px; /* Set your desired font size */\n"
                //"}"
        );

        barChart.setPrefHeight(paneWithGraph.getHeight());
        barChart.setMaxHeight(paneWithGraph.getHeight());

        paneWithGraph.getChildren().add(barChart);

        paneWithGraph.layoutBoundsProperty().addListener((observable, oldValue, newValue) -> {
            double h = paneWithGraph.getHeight() * 8 / 10;
            barChart.setMaxHeight(h);
            barChart.setPrefHeight(h);
            barChart.setMinHeight(h);
        });
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

        this.testPlayers();

        Platform.runLater(() -> {
            Collection<PlayerHandler> players;
            players = ((SharedSessionData) stageManager.getUserData()).getServerSocket().getPlayerHandlers().values();
            PlayerHandler[] sortedPlayers = players.toArray(new PlayerHandler[0]);
            Arrays.sort(sortedPlayers, (o1, o2) -> Integer.compare(o2.getPoints(), o1.getPoints()));
            System.out.println(Arrays.toString(sortedPlayers));


            XYChart.Series<String, Number> dataSeries1 = new XYChart.Series<>();
            for (int i = 0; i < 10; i++) {
                int v;
                StringBuilder s;
                if (i < sortedPlayers.length) {
                    PlayerHandler handler;
                    handler = sortedPlayers[i];
                    v = handler.getPoints();
                    s = new StringBuilder(handler.getName());
                }
                else
                {
                    v = 0;
                    s = new StringBuilder();
                    for (int j = 0; j < i; j++)
                        s.append("⠀");
                }
                dataSeries1.getData().add(new XYChart.Data<>(s.toString(), v));
            }

            for (XYChart.Data<String, Number> data : dataSeries1.getData()) {
                System.out.println(data.getXValue() + ": " + data.getYValue());
            }

            barChart.setAnimated(true);
            barChart.getData().clear();

            barChart.getData().add(dataSeries1);
        });
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
