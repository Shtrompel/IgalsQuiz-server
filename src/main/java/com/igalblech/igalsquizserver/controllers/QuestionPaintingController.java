package com.igalblech.igalsquizserver.controllers;

import com.igalblech.igalsquizserver.InterfaceController;
import com.igalblech.igalsquizserver.Questions.Answer;
import com.igalblech.igalsquizserver.Questions.QuestionBase;
import com.igalblech.igalsquizserver.Questions.QuestionMinigame;
import com.igalblech.igalsquizserver.Questions.QuestionPainting;
import com.igalblech.igalsquizserver.QuizApplication;
import com.igalblech.igalsquizserver.SharedSessionData;
import com.igalblech.igalsquizserver.Utils;
import com.igalblech.igalsquizserver.network.PlayerHandler;
import javafx.animation.*;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Scanner;

public class QuestionPaintingController implements InterfaceController, InterfaceQuestion {

    SceneManager manager;
    Application app;

    @FXML
    ImageView imageQuestionImage;
    @FXML
    Text textQuestionTitle;
    @FXML
    Text textQuestionDescription;
    @FXML
    Text textQuestionTime;
    @FXML
    Button buttonFinishQuestion, buttonFinishTimer;

    @FXML
    ImageView imageViewSubmission;
    @FXML
    Button btnImagePrev, btnImageNext;
    @FXML
    Button btnRate1, btnRate2, btnRate3, btnRate4, btnRate5;
    @FXML
    VBox boxPaintingRating;

    Button[] btnsRate;
    String[] btnsSelectColor = {"#ff0000", "#ff7f7f", "#7f7f7f", "#ffff7f", "#ffff00"};

    @Nullable
    Integer[] rating = null;

    private long questionTimeLimit = -1;
    private long questionTimeStart;
    AnimationTimer timerQuestion = null;
    ParallelTransition countdownStart = null;

    public static final boolean TESTING = false;

    int playerVotingViewIndex = 0;


    public void initialize() {
        System.out.println("QuestionPaintingController initialize()");

        this.btnsRate = new Button[]{btnRate1, btnRate2, btnRate3, btnRate4, btnRate5};

        for (int i = 0; i < 5; i++) {
            URL url = QuizApplication.class.getResource("");
            assert url != null;
            String path = "file://" + url.getPath() + "imgs/scale" + (i + 1) + ".png";
            Image img = new Image(path);

            ImageView imgView = new ImageView();
            imgView.setImage(img);
            imgView.setFitWidth(60);
            imgView.setFitHeight(60);
            imgView.setPreserveRatio(false);

            btnsRate[i].setGraphic(imgView);
            final int index = i;
            btnsRate[i].setOnMousePressed(event -> buttonVotePressed(index));
        }
    }

    public void onButtonFinishQuestionPressed() {
        Object[] objArray = ((QuizApplication) app).getServerSocket().getPlayerHandlers().values().toArray();
        PlayerHandler[] players = Arrays.copyOf(objArray, objArray.length, PlayerHandler[].class);

        var data = ((SharedSessionData) manager.getUserData());

        for (int i = 0; i < players.length; i++) {
            PlayerHandler playerHandler = players[i];
            int r = rating[i];

            Answer.Validity validity = Answer.Validity.PARTIALLY_RIGHT;
            if (r == 0)
                validity = Answer.Validity.WRONG;
            else if (r == 4)
                validity = Answer.Validity.RIGHT;

            Answer answer = playerHandler.getAnswer();
            answer.setPoints((int) (data.getCurrent().getPoints() * ((double) r / 4)));
            answer.setValidity(validity);
            answer.setValidable(false);
        }

        data.sendQuestionEnd();

        manager.changeScene("QUESTION_TRANSITION");
    }

    public void onButtonFinishTimerPressed() {
        onSendQuestionEnd();
    }

    public void btnImagePrevPressed() {
        setCurrentVotingPlayer(playerVotingViewIndex - 1);
    }

    public void btnImageNextPressed() {
        setCurrentVotingPlayer(playerVotingViewIndex + 1);
    }

    private void sendQuestionStart() {
        if (TESTING) return;
        SharedSessionData data = (SharedSessionData) manager.getUserData();
        data.getServerSocket().sendQuestionStart(data.getCurrent());
    }

    @Override
    public void onShown() {
        System.out.println("QuestionPaintingController onShown()");

        buttonFinishQuestion.setVisible(false);
        buttonFinishTimer.setVisible(true);
        textQuestionTime.setVisible(true);
        boxPaintingRating.setVisible(false);
        imageQuestionImage.setVisible(true);

        // Begin the countdown
        questionTimeStart = System.currentTimeMillis();

        // Send all data to the player
        sendQuestionStart();


        // If no time limit has made:
        if (questionTimeLimit == 0) {
            textQuestionTime.setVisible(false);
            return;
        }

        if (TESTING) {
            Map<String, PlayerHandler> players;
            players = ((QuizApplication) app).getServerSocket().getPlayerHandlers();
            for (int i = 0; i < 5; i++) {
                String str = "";
                try {
                    Scanner in = new Scanner(new FileReader("test" + (i + 1) + ".txt"));

                    StringBuilder sb = new StringBuilder();
                    while (in.hasNext()) {
                        sb.append(in.next());
                    }
                    in.close();
                    str = sb.toString();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                Image image = Utils.decodeBase64ToImage(str);
                imageViewSubmission.setImage(image);
                PlayerHandler playerHandler = new PlayerHandler();
                playerHandler.setUuid("name" + i);
                playerHandler.setName("name" + i);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("image", str);

                Answer answer = new Answer();
                answer.setExternalData(jsonObject);
                playerHandler.setAnswer(answer);
                players.put(playerHandler.getUuid(), playerHandler);
            }
        }

        // If there is a time limit:

        timerQuestion = new AnimationTimer() {
            @Override
            public void handle(long now) {
                long elapsedMillis = System.currentTimeMillis() - questionTimeStart;
                long timePassed = questionTimeLimit - elapsedMillis;

                if (timePassed < 0) {
                    timerQuestion.stop();
                    onSendQuestionEnd();
                }

                long seconds = timePassed / 1000;
                textQuestionTime.setText("Time Left:\n" +
                        String.format("%02d:%02d", seconds / 60, seconds % 60));
            }
        };

        timerQuestion.start();
    }

    @Override
    public void onClose() {
        if (timerQuestion != null) {
            timerQuestion.stop();
        }
        timerQuestion = null;

        if (countdownStart != null) {
            countdownStart.jumpTo(Duration.ZERO);
            countdownStart.stop();
        }
        countdownStart = null;
    }

    @Override
    public void onKeyboardButtonPressed(KeyEvent e) {

    }

    @Override
    public void onSendQuestionEnd() {
        buttonFinishTimer.setVisible(false);
        buttonFinishQuestion.setVisible(true);
        boxPaintingRating.setVisible(true);
        textQuestionTime.setVisible(false);
        imageQuestionImage.setVisible(false);

        var players = ((QuizApplication) app).getServerSocket().getPlayerHandlers();
        if (players.isEmpty())
            return;
        rating = new Integer[players.size()];

        setCurrentVotingPlayer(playerVotingViewIndex);
    }

    private void setCurrentVotingPlayer(int index) {
        var players = ((QuizApplication) app).getServerSocket().getPlayerHandlers();
        if (index < 0 || index >= players.size())
            return;

        this.playerVotingViewIndex = index;
        PlayerHandler playerHandler = (PlayerHandler) players.values().toArray()[index];
        Answer answer = playerHandler.getAnswer();
        if (answer == null)
            return;
        JSONObject json = answer.getExternalData();
        String imageBase64 = json.getString("image");
        Image image = Utils.decodeBase64ToImage(imageBase64);
        imageViewSubmission.setImage(image);

        for (int i = 0; i < btnsRate.length; i++)
            buttonVoteColorUnpress(i);

        if (rating[index] != null)
            buttonVoteColorPress(rating[index]);
    }

    private void buttonVotePressed(int v) {
        for (int i = 0; i < btnsRate.length; i++)
            buttonVoteColorUnpress(i);
        buttonVoteColorPress(v);
        if (playerVotingViewIndex < rating.length)
            rating[playerVotingViewIndex] = v;
    }

    void buttonVoteColorPress(int i) {
        btnsRate[i].setStyle(String.format(
                "-fx-background-color: %s; ",
                btnsSelectColor[i]));
    }

    void buttonVoteColorUnpress(int i) {
        btnsRate[i].setStyle("-fx-background-color: #ffffff; ");
    }


    @Override
    public void setScene(SceneManager manager) {
        this.manager = manager;
    }

    @Override
    public void setApp(Application app) {
        this.app = app;
    }


    @Override
    public void applyQuestion(QuestionBase questionsBase) {
        System.out.println("QuestionGameController applyQuestion(QuestionBase questionsBase)");

        if (!(questionsBase instanceof QuestionPainting)) {
            System.out.println(
                    "Wrong type of question in QuestionController. " +
                            "Type should be QuestionPainting");
            return;
        }

        if (imageQuestionImage != null)
            imageQuestionImage.setImage(questionsBase.getImage());
        if (textQuestionTitle != null)
            textQuestionTitle.setText(questionsBase.getTitle());
        if (textQuestionDescription != null)
            textQuestionDescription.setText(questionsBase.getDescription());

        questionTimeLimit = questionsBase.getTimeLimit();

    }
}