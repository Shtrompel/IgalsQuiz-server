package com.igalblech.igalsquizserver.controllers;

import com.igalblech.igalsquizserver.Questions.QuestionBase;
import com.igalblech.igalsquizserver.Questions.QuestionMinigame;
import com.igalblech.igalsquizserver.SharedSessionData;
import javafx.animation.*;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class QuestionGameController implements InterfaceController, InterfaceQuestion {

    SceneManager manager;
    Application app;

    @FXML
    ImageView imageQuestionImage;
    @FXML
    Text textQuestionTitle;
    @FXML
    Text textQuestionDescription;
    @FXML
    Rectangle rectangleCover;
    @FXML
    Text textQuestionTime;
    @FXML
    Button buttonFinishQuestion;

    private long questionTimeLimit = -1;
    private long questionTimeStart;
    AnimationTimer timerQuestion = null;
    ParallelTransition countdownStart = null;

    private String gameName;
    private int gameDifficulty;
    private QuestionMinigame question;

    public void initialize()
    {
        System.out.println("QuestionGameController initialize()");
    }

    public void onButtonFinishQuestionPressed()
    {
        onSendQuestionEnd();
    }

    private void sendQuestionStart() {
        SharedSessionData data = (SharedSessionData)manager.getUserData();
        System.out.println(data.getCurrent().toJson().toString());
        data.getServerSocket().sendQuestionStart(data.getCurrent());
    }

    @Override
    public void onShown()
    {
        System.out.println("QuestionController onShown()");

        textQuestionTime.setVisible(false);
        buttonFinishQuestion.setVisible(false);

        // Begin the countdown
        questionTimeStart = System.currentTimeMillis();

        // Send all data to the player
        sendQuestionStart();

        textQuestionTime.setVisible(true);
        buttonFinishQuestion.setVisible(true);

        // If no time limit has made:
        if (questionTimeLimit == 0)
        {
            textQuestionTime.setVisible(false);
            return;
        }

        if (!gameName.equals("Horror"))
        {
            ((SharedSessionData)this.manager.getUserData()).playMusicQuestion();
        }
        else
        {
            ((SharedSessionData)this.manager.getUserData()).stopMusic();
        }

        // If there is a time limit:

        timerQuestion = new AnimationTimer() {
            @Override
            public void handle(long now) {
                long elapsedMillis = System.currentTimeMillis() - questionTimeStart;
                long timePassed = questionTimeLimit - elapsedMillis;

                if (timePassed < 0) {
                    timerQuestion.stop();
                    if (question.isForceEnd())
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
        SharedSessionData data = (SharedSessionData)manager.getUserData();
        data.sendQuestionEnd();

        manager.changeScene("QUESTION_TRANSITION");
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

        if (!(questionsBase instanceof QuestionMinigame question))
        {
            System.out.println(
                    "Wrong type of question in QuestionController. " +
                            "Type should be QuestionMinigame");
            return;
        }

        if (questionsBase.getImage() != null) {
            imageQuestionImage.setFitWidth(questionsBase.getImage().getWidth());
            imageQuestionImage.setFitHeight(questionsBase.getImage().getHeight());
            imageQuestionImage.setImage(questionsBase.getImage());
        }
        textQuestionTitle.setText(questionsBase.getTitle());
        textQuestionDescription.setText(questionsBase.getDescription());

        questionTimeLimit = questionsBase.getTimeLimit();

        this.gameDifficulty = question.getGameDifficulty();
        this.gameName = question.getGameName();

        this.question = question;
    }
}
