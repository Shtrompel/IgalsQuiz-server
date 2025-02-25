package com.igalblech.igalsquizserver.controllers;

import com.igalblech.igalsquizserver.InterfaceController;
import com.igalblech.igalsquizserver.Questions.QuestionBase;
import com.igalblech.igalsquizserver.Questions.QuestionMinigame;
import com.igalblech.igalsquizserver.QuizApplication;
import com.igalblech.igalsquizserver.SharedSessionData;
import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.net.URL;

public class QuestionIntroController implements InterfaceController {

    public static final int TIME_START = 8;
    boolean skipIntro = false;

    SceneManager manager;
    Application app;

    @FXML
    Text labelCountdown;
    @FXML
    Text labelCoverQuestion;
    @FXML
    Text labelDescription;
    @FXML
    ImageView imageQuestion;
    @FXML
    StackPane stackPaneIntro;

    ParallelTransition countdownStart = null;
    int timeStart;

    AudioClip audioScaryHit;

    private QuestionBase questionBase;

    public void initialize()
    {
        URL url = QuizApplication.class.getResource("");
        audioScaryHit = new AudioClip("file://" + url.getPath() + "audio/scaryhit.wav");
    }

    private void sendQuestionData() {
        SharedSessionData data = (SharedSessionData)manager.getUserData();
        data.getServerSocket().sendQuestionData();
    }

    void postIntroInit() {
        {
            String sceneName;
            sceneName = QuizApplication.questionTypeToSceneName(
                    this.questionBase.getQuestionType());

            InterfaceController controller;
            controller = manager.getController(sceneName);

            InterfaceQuestion ic;
            ic = (InterfaceQuestion) controller;
            assert ic != null;

            ic.applyQuestion(this.questionBase);
        }

        String sceneName = QuizApplication.questionTypeToSceneName(
                this.questionBase.getQuestionType());

        if (sceneName.equals(""))
            throw new IllegalArgumentException(String.format("Can't find scene for question type %s", sceneName));

        manager.changeScene(sceneName);
    }

    @Override
    public void onShown() {

        SharedSessionData questionsSet = (SharedSessionData) manager.getUserData();
        if (!questionsSet.hasNext())
        {
            manager.changeScene("QUIZ_END");
            return;
        }

        this.questionBase = questionsSet.getNext();

        sendQuestionData();

        if (skipIntro)
        {
            postIntroInit();
            return;
        }

        this.labelCoverQuestion.setText(this.questionBase.getTitle());
        this.labelDescription.setText(this.questionBase.getDescription());

        this.imageQuestion.setFitWidth(this.questionBase.getImage().getWidth());
        this.imageQuestion.setFitHeight(this.questionBase.getImage().getHeight());
        this.imageQuestion.setImage(this.questionBase.getImage());

        labelCountdown.setVisible(true);
        labelCoverQuestion.setVisible(true);

        this.timeStart = TIME_START;
        labelCountdown.setText(String.valueOf(timeStart));

        countdownStart = getStartingTimeline();
        countdownStart.play();

    }

    private ParallelTransition getStartingTimeline() {

        var countdown = getTimerTimeline();
        var questionSequence = getTitleTimeline();
        var description = getDescriptionTimeline();
        var image = getImageTimeline();

        ParallelTransition transition = new ParallelTransition(
                countdown, questionSequence, image, description);
        transition.setOnFinished(event -> transition.jumpTo(Duration.ZERO));

        return transition;
    }

    private Animation getTimerTimeline()
    {
        Timeline countdown = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> {
                    timeStart -= 1;
                    labelCountdown.setVisible(true);
                    labelCountdown.setText(String.valueOf(timeStart));
                })
        );
        countdown.setCycleCount(timeStart);
        countdown.setOnFinished(event -> postIntroInit());

        return countdown;
    }

    private Animation getDescriptionTimeline()
    {
        double timePause = TIME_START / 5.0 * 2.0;
        double timeScale = TIME_START / 5.0;

        PauseTransition questionPause = new PauseTransition(Duration.seconds(timePause));
        double originalScaleX = labelDescription.getScaleX();
        double originalScaleY = labelDescription.getScaleY();

        labelDescription.setScaleX(0.0);
        labelDescription.setScaleY(0.0);

        ScaleTransition scale = new ScaleTransition(Duration.seconds(timeScale), labelDescription);
        scale.setByX(1.0);
        scale.setByY(1.0);

        SequentialTransition sequence = new SequentialTransition(
                questionPause,
                scale);
        sequence.setOnFinished(event -> {
            labelDescription.setScaleX(originalScaleX);
            labelDescription.setScaleY(originalScaleY);
        });

        return sequence;
    }

    private Animation getImageTimeline()
    {
        double timePause = TIME_START / 5.0 * 2.0;
        double timeScale = TIME_START / 5.0;

        PauseTransition questionPause = new PauseTransition(Duration.seconds(timePause));

        questionPause.setOnFinished(event -> {
            if (this.questionBase.getQuestionType().equals("QuestionMinigame") &&
                    ((QuestionMinigame)this.questionBase).getGameName().equals("Horror"))
            {
                audioScaryHit.play();
                stackPaneIntro.setStyle("-fx-background-color: #6d0000;");
            }
        });

        double originalScaleX = imageQuestion.getScaleX();
        double originalScaleY = imageQuestion.getScaleY();

        imageQuestion.setScaleX(0.0);
        imageQuestion.setScaleY(0.0);

        ScaleTransition scale = new ScaleTransition(Duration.seconds(timeScale), imageQuestion);
        scale.setByX(1.0);
        scale.setByY(1.0);

        SequentialTransition sequence = new SequentialTransition(
                questionPause,
                scale);
        sequence.setOnFinished(event -> {
            imageQuestion.setScaleX(originalScaleX);
            imageQuestion.setScaleY(originalScaleY);
        });

        return sequence;
    }

    private Animation getTitleTimeline()
    {
        double timeScale = TIME_START / 5.0;
        double timePause1 = TIME_START / 5.0 * 0.5;
        double timeUp = TIME_START / 5.0 * 0.5;
        double timePause2 = TIME_START / 5.0 * 2.0;
        double timeFly2 = TIME_START / 5.0;

        double timeFly = 0.5;

        labelCoverQuestion.setScaleX(0.0);
        labelCoverQuestion.setScaleY(0.0);


        Interpolator bounceInterpolator = new Interpolator() {
            @Override
            protected double curve(double t) {
                if (t < 0.36364) {
                    return 7.5625 * t * t;
                } else if (t < 0.72727) {
                    t -= 0.54545;
                    return 7.5625 * t * t + 0.75;
                } else if (t < 0.90909) {
                    t -= 0.81818;
                    return 7.5625 * t * t + 0.9375;
                } else {
                    t -= 0.95455;
                    return 7.5625 * t * t + 0.984375;
                }
            }
        };

        double originalX = labelCoverQuestion.getTranslateX();
        double originalY = labelCoverQuestion.getTranslateY();
        double originalScaleX = labelCoverQuestion.getScaleX();
        double originalScaleY = labelCoverQuestion.getScaleY();
        double originalRotation = labelCoverQuestion.getRotate();

        ScaleTransition questionScale = new ScaleTransition(Duration.seconds(timeScale), labelCoverQuestion);
        questionScale.setByX(1.0);
        questionScale.setByY(1.0);
        questionScale.setInterpolator(bounceInterpolator);

        PauseTransition questionPauseA = new PauseTransition(Duration.seconds(timePause1));

        TranslateTransition moveUp = new TranslateTransition(Duration.seconds(timeUp), labelCoverQuestion);
        moveUp.setToY(-300);

        PauseTransition questionPauseB = new PauseTransition(Duration.seconds(timePause2));

        TranslateTransition questionFlyP = new TranslateTransition(Duration.seconds(timeFly), labelCoverQuestion);
        questionFlyP.setByX(Math.signum(Math.random() - 0.5) * (Math.random() * 1000 + 100));
        questionFlyP.setByY(Math.random() * 1000);
        RotateTransition questionFlyR = new RotateTransition(Duration.seconds(timeFly2), labelCoverQuestion);
        questionFlyR.setByAngle(2 * (Math.random() - 0.5) * 360 * 5);
        ParallelTransition questionFly = new ParallelTransition(questionFlyP, questionFlyR);

        SequentialTransition questionSequence = new SequentialTransition(
                questionScale,
                questionPauseA,
                moveUp,
                questionPauseB,
                questionFly);
        questionSequence.setOnFinished(event -> {
            labelCoverQuestion.setTranslateX(originalX);
            labelCoverQuestion.setTranslateY(originalY);
            labelCoverQuestion.setScaleX(originalScaleX);
            labelCoverQuestion.setScaleY(originalScaleY);
            labelCoverQuestion.setRotate(originalRotation);
        });

        return questionSequence;
    }


    @Override
    public void setScene(SceneManager stage) {
        this.manager = stage;
    }

    @Override
    public void setApp(Application app) {
        this.app = app;
    }

    @Override
    public void onClose() {
        stackPaneIntro.setStyle("");
    }

    @Override
    public void onKeyboardButtonPressed(KeyEvent e) {

    }

    @Override
    public void onSendQuestionEnd() {

    }

}
