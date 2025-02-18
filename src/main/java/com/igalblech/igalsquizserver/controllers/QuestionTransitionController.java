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
    private Application app;
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
            double width = newValue.getWidth();
            double height = newValue.getHeight();

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
        this.app = app;
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


        Collection<PlayerHandler> players;
        players = ((SharedSessionData) stageManager.getUserData()).getServerSocket().getPlayerHandlers().values();
        /*
        players = new ArrayList<>();
        players.clear();
        for (int i = 0 ; i < 12; i++)
        {
            PlayerHandler h = new PlayerHandler(null, null);
            h.addPoints((int) (Math.random()*1000));
            h.setName("user"+i);
            players.add(h);
        }*/

        List<PlayerHandler> sortedPlayers = new ArrayList<>(players);
        Collections.sort(sortedPlayers, (o1, o2) -> {
            if (o1.getPoints() == o2.getPoints())
                return 0;
            return (o1.getPoints() > o2.getPoints()) ? -1 : (1);
        });


        XYChart.Series dataSeries1 = new XYChart.Series();

        for (int j = 0; j < 10; j++) {
            dataSeries1.getData().add(new XYChart.Data("", 0));
        }

        int i = 0;
        for (PlayerHandler handler : sortedPlayers) {
            int index = 10 - ++i;
            if (index < 0)
                continue;
            int v = handler.getPoints();
            String s = handler.getName();
            dataSeries1.getData().set(index, new XYChart.Data(s, v)); // Ensure the index is valid
        }

        Platform.runLater(() -> {
            barChart.getData().clear();
            barChart.getData().add(dataSeries1);
        });

        /*

        ((SharedSessionData)stageManager.getUserData()).getServerSocket().getPlayerHandlers();

        XYChart.Series dataSeries1 = new XYChart.Series();

        barChart.getData().clear();
        barChart.getData().add(dataSeries1);

        int v = 0;
        for (int i = 0; i < 10; i++) {
            dataSeries1.getData().add(i, new XYChart.Data("", 0));
        }
        for (int i = 0; i < 10; i++) {
            v += (int) (Math.random() * 100);
            String s = "user" + i + 1;
            dataSeries1.getData().set(10 - i - 1, new XYChart.Data(s, v));
        }
         */


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
