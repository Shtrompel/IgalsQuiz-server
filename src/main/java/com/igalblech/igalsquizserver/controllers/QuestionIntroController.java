package com.igalblech.igalsquizserver.controllers;

import com.igalblech.igalsquizserver.InterfaceController;
import com.igalblech.igalsquizserver.Questions.QuestionBase;
import com.igalblech.igalsquizserver.QuizApplication;
import com.igalblech.igalsquizserver.SharedSessionData;
import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class QuestionIntroController implements InterfaceController {

    public static final int TIME_START = 3;
    boolean skipIntro = false;

    SceneManager manager;
    Application app;

    @FXML
    Text labelCountdown;
    @FXML
    Text labelCoverQuestion;

    ParallelTransition countdownStart = null;
    int timeStart;

    private QuestionBase questionBase;

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

        labelCountdown.setVisible(true);
        labelCoverQuestion.setVisible(true);

        this.timeStart = TIME_START;
        labelCountdown.setText(String.valueOf(timeStart));

        countdownStart = getStartingTimeline();
        countdownStart.play();

    }

    private ParallelTransition getStartingTimeline() {
        Timeline countdown = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> {
                    timeStart -= 1;
                    labelCountdown.setVisible(true);
                    labelCountdown.setText(String.valueOf(timeStart));
                })
        );
        countdown.setCycleCount(timeStart);
        countdown.setOnFinished(event -> postIntroInit());

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

        ScaleTransition questionScale = new ScaleTransition(Duration.seconds(TIME_START/5.0*3), labelCoverQuestion);
        questionScale.setByX(1.0);
        questionScale.setByY(1.0);
        questionScale.setInterpolator(bounceInterpolator);

        PauseTransition questionPause = new PauseTransition(Duration.seconds(TIME_START/5.0*1.5));

        TranslateTransition questionFlyP = new TranslateTransition(Duration.seconds(0.5), labelCoverQuestion);
        questionFlyP.setByX(Math.signum(Math.random() - 0.5) * (Math.random() * 1000 + 100));
        questionFlyP.setByY(Math.random() * 1000);
        RotateTransition questionFlyR = new RotateTransition(Duration.seconds(TIME_START/5.0*0.5), labelCoverQuestion);
        questionFlyR.setByAngle(2 * (Math.random() - 0.5) * 360 * 5);
        ParallelTransition questionFly = new ParallelTransition(questionFlyP, questionFlyR);

        SequentialTransition questionSequence = new SequentialTransition(questionScale, questionPause, questionFly);
        questionSequence.setOnFinished(event -> {
            labelCoverQuestion.setTranslateX(originalX);
            labelCoverQuestion.setTranslateY(originalY);
            labelCoverQuestion.setScaleX(originalScaleX);
            labelCoverQuestion.setScaleY(originalScaleY);
            labelCoverQuestion.setRotate(originalRotation);
        });


        ParallelTransition transition = new ParallelTransition(countdown, questionSequence);
        transition.setOnFinished(event -> transition.jumpTo(Duration.ZERO));

        return transition;
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

    }

    @Override
    public void onKeyboardButtonPressed(KeyEvent e) {

    }

    @Override
    public void onSendQuestionEnd() {

    }

}
