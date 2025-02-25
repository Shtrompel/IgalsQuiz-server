package com.igalblech.igalsquizserver.controllers;

import com.igalblech.igalsquizserver.InterfaceController;
import com.igalblech.igalsquizserver.Questions.Choice;
import com.igalblech.igalsquizserver.Questions.QuestionBase;
import com.igalblech.igalsquizserver.Questions.QuestionChoice;
import com.igalblech.igalsquizserver.Questions.QuestionOrder;
import com.igalblech.igalsquizserver.SharedSessionData;
import com.igalblech.igalsquizserver.Utils;
import javafx.animation.*;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import com.igalblech.igalsquizserver.ui.ChoiceNode;
import javafx.util.Duration;

import java.util.*;

public class QuestionController implements InterfaceController, InterfaceQuestion {

    SceneManager manager;
    Application app;

    @FXML
    ImageView imageQuestionImage;
    @FXML
    Text textQuestionTitle;
    @FXML
    Text textQuestionDescription;
    @FXML
    GridPane gridChoices;
    @FXML
    Rectangle rectangleCover;
    @FXML
    Text textQuestionTime;
    @FXML
    Button buttonFinishQuestion;
    @FXML
    StackPane stackPaneQuestion;

    private long questionTimeLimit = -1;
    private long questionTimeStart;
    AnimationTimer timerQuestion = null;
    ParallelTransition countdownStart = null;

    private SequentialTransition choicesAnimation = null;

    public void initialize()
    {
        System.out.println("QuestionController initialize()");
    }

    void startQuestionTimer()
    {
        System.out.println("QuestionController postIntroInit()");

        this.choicesAnimation = null;

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

    public void onButtonFinishQuestionPressed()
    {
        onSendQuestionEnd();
    }

    public void applyQuestion(QuestionBase questionsBase)
    {
        System.out.println("QuestionController applyQuestion(QuestionBase questionsBase)");

        imageQuestionImage.setImage(questionsBase.getImage());
        textQuestionTitle.setText(questionsBase.getTitle());
        textQuestionDescription.setText(questionsBase.getDescription());

        questionTimeLimit = questionsBase.getTimeLimit();


        if ((questionsBase instanceof QuestionOrder question)) {
            List<Choice> choiceList = question.getAnswersOut();
            System.out.println("QuestionOrder: " + question.isRandomize());

            double chroma = 0.3;
            for (int i = 0; i < choiceList.size(); i++) {
                Choice c = choiceList.get(i);
                if (c.getColor().length != 0)
                    continue;

                int j = i / 2;
                if (i % 2 == 1)
                    j += choiceList.size() / 2;

                double n = ((float) j) / (choiceList.size() - 1.);

                double hue = 2 * Math.PI * n;
                double a = chroma * Math.cos(hue);
                double b = chroma * Math.sin(hue);
                c.setColor(Utils.oklabToSRGB(0.7, a, b));
            }

            fillChoicesGrid(choiceList);
        }

        if ((questionsBase instanceof QuestionChoice question)) {

            List<Choice> choiceList = new ArrayList<>(question.getChoices());
            if (question.isRandomize())
                Collections.shuffle(choiceList);

            double chroma = 0.3;
            for (int i = 0; i < choiceList.size(); i++) {
                Choice c = choiceList.get(i);
                if (c.getColor().length != 0)
                    continue;

                int j = i / 2;
                if (i % 2 == 1)
                    j += choiceList.size() / 2;

                double n = ((float) j) / (choiceList.size() - 1.);

                double hue = 2 * Math.PI * n;
                double a = chroma * Math.cos(hue);
                double b = chroma * Math.sin(hue);
                c.setColor(Utils.oklabToSRGB(0.7, a, b));
            }

            fillChoicesGrid(choiceList);
        }
        else{
            System.out.println(
                    "Wrong type of question in QuestionController.");
        }
    }

    private void sendQuestionData() {
        SharedSessionData data = (SharedSessionData)manager.getUserData();
        data.getServerSocket().sendQuestionData();
    }

    private void sendQuestionStart() {
        SharedSessionData data = (SharedSessionData)manager.getUserData();
        data.getServerSocket().sendQuestionStart(data.getCurrent());
    }

    void fillChoicesGrid(List<Choice> choiceList) {
        var gridPane = gridChoices;

        gridPane.setGridLinesVisible(false);
        gridPane.getColumnConstraints().clear();
        gridPane.getRowConstraints().clear();
        gridPane.getChildren().clear();
        gridPane.setGridLinesVisible(true);

        int[] grid = Utils.findClosestProduct(choiceList.size());

        int numColumns = grid[1];
        //int numRows = grid[0];

        // Add ChoiceNodes to the GridPane
        for (int i = 0; i < choiceList.size(); i++) {
            Choice choice = choiceList.get(i);
            int[] arrCol = choice.getColor();

            Color c = null;
            if (arrCol.length >= 3)
                c = new Color(arrCol[0] / 255., arrCol[1] / 255., arrCol[2] / 255., 1.0);

            ChoiceNode choiceNode = new ChoiceNode(
                    gridPane,
                    choice.getText(),
                    choice.getImage(),
                    c,
                    choice.getSound());

            int column = i % numColumns;
            int row = i / numColumns;

            GridPane.setFillWidth(choiceNode, true);
            GridPane.setFillHeight(choiceNode, true);

            // Add the ChoiceNode to the GridPane
            gridPane.add(choiceNode, column, row);
        }
    }

    private SequentialTransition animateAllChoices(GridPane gridPane) {
        SequentialTransition allAnimations = new SequentialTransition();

        boolean any = false;

        // Iterate over the GridPane's children
        for (Node node : gridPane.getChildren()) {
            if (node instanceof ChoiceNode choiceNode) {

                if (choiceNode.getMedia() == null)
                    continue;

                any = true;

                // Animate each ChoiceNode
                SequentialTransition choiceAnimation = animateChoiceNode(choiceNode);
                allAnimations.getChildren().add(choiceAnimation);
            }
        }

        if (!any)
            return null;

        // Add a listener for when all animations are finished
        allAnimations.setOnFinished(e -> {
            this.choicesAnimation = null;
            startQuestionTimer();
        });

        return allAnimations;
    }

    private static SequentialTransition animateChoiceNode(ChoiceNode choiceNode) {
        // Scale animation to grow and shrink the ChoiceNode

        PauseTransition onTop = new PauseTransition(Duration.millis(0));
        onTop.setOnFinished(e -> choiceNode.toFront());

        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(500), choiceNode);
        scaleUp.setToX(1.4);
        scaleUp.setToY(1.4);

        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(500), choiceNode);
        scaleDown.setToX(1.0);
        scaleDown.setToY(1.0);

        // MediaPlayer for sound
        final MediaPlayer mediaPlayer;
        if (choiceNode.getMedia() != null) {
            mediaPlayer = new MediaPlayer(choiceNode.getMedia());
        } else {
            mediaPlayer = null;
        }

        PauseTransition playSound = new PauseTransition(Duration.millis(0));
        playSound.setOnFinished(e -> {
            choiceNode.highlight(Color.YELLOW);
            if (mediaPlayer != null) {
                mediaPlayer.play();
            }
        });

        // Play sound and highlight
        PauseTransition pause = new PauseTransition(Duration.millis(500));

        // Reset highlight
        PauseTransition resetPause = new PauseTransition(Duration.millis(500));
        resetPause.setOnFinished(e -> choiceNode.resetHighlight());

        // Combine animations into a sequence
        SequentialTransition sequence = new SequentialTransition();
        sequence.getChildren().addAll(onTop, scaleUp, playSound, pause, scaleDown, resetPause);
        return sequence;
    }

    @Override
    public void onShown()
    {
        System.out.println("QuestionController onShown()");

        sendQuestionData();

        textQuestionTime.setVisible(false);
        buttonFinishQuestion.setVisible(false);

        // On Scene Opened Stuff
        choicesAnimation = animateAllChoices(gridChoices);
        if (choicesAnimation != null) {
            choicesAnimation.play();
        }
        else
        {
            startQuestionTimer();
        }
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

        if (choicesAnimation != null) {
            choicesAnimation.jumpTo(Duration.ZERO);
            choicesAnimation.stop();
        }
        choicesAnimation = null;
    }

    @Override
    public void onKeyboardButtonPressed(KeyEvent e) {
        if (e.getCode() == KeyCode.SPACE) {
            if (choicesAnimation != null) {
                choicesAnimation.jumpTo(Duration.millis(0));
                choicesAnimation.stop();
                startQuestionTimer();
            }
            choicesAnimation = null;
        }
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


}
