package com.igalblech.igalsquizserver;

import com.igalblech.igalsquizserver.Questions.Answer;
import com.igalblech.igalsquizserver.Questions.QuestionBase;
import com.igalblech.igalsquizserver.controllers.SceneManager;
import com.igalblech.igalsquizserver.network.PlayerHandler;
import com.igalblech.igalsquizserver.network.QuizServerSocket;
import com.igalblech.igalsquizserver.utils.UserData;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;

public class SharedSessionData implements UserData {

    public static final double VOLUME = 1;

    @Getter @Setter
    private String ipAddress = "";

    @Getter
    private List<QuestionBase> questionsList;

    private QuestionBase currentQuestion = null;
    private ListIterator<QuestionBase> itrQuestions;

    @Getter
    private Properties properties;

    @Getter
    private QuizServerSocket serverSocket;

    @Setter
    private OnGetSceneManager onGetSceneManager = null;

    private AudioClip musicMenu;
    private AudioClip musicQuestion;
    private MediaPlayer sfxEndQuestion;

    SharedSessionData(QuizServerSocket serverSocket, Properties properties) {
        this.serverSocket = serverSocket;
        this.properties = properties;

        try {
            this.musicMenu = new AudioClip(QuizApplication.getFileURL("audio/lobbymusic.mp3").toURI().toString());
            musicMenu.setCycleCount(16);
            this.musicQuestion = new AudioClip(QuizApplication.getFileURL("audio/qestion.mp3").toURI().toString());
            this.musicQuestion.setCycleCount(16);
            {
                Media media = new Media(QuizApplication.getFileURL("audio/answer.mp3").toURI().toString());
                this.sfxEndQuestion = new MediaPlayer(media);
                this.sfxEndQuestion.setCycleCount(2);
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        musicMenu.setVolume(VOLUME);
        musicQuestion.setVolume(VOLUME);

        this.serverSocket.setOnGetAnswer((id, answerStr) -> {

            PlayerHandler handler;
            handler = serverSocket.getPlayerHandlers().get(id);
            if (handler == null)
                return;

            Answer answer = null;
            if (currentQuestion != null) {
                answer = currentQuestion.compareAnswer(new JSONObject(answerStr));
            }

            System.out.println("Got answer: " + answer.toString());
            handler.setAnswer(answer);
            System.out.println("Got answer: " + handler.toString());


            if (onGetSceneManager != null) {
                boolean allAnswered = true;
                for (var ph : serverSocket.getPlayerHandlers().values()) {
                    //System.out.println(ph.getName() + ", " + ph.getAnswer());
                    if (ph.getAnswer() == null && ph.isActive()) {
                        allAnswered = false;
                        break;
                    }
                }

                if (allAnswered) {
                    SceneManager manager = onGetSceneManager.getSceneManager();
                    manager.getCurrentScene().getController().onSendQuestionEnd();
                }
            }

        });
    }

    public void playMusicMenu()
    {
        stopMusic();
        Platform.runLater(() -> {
            musicMenu.play();
        });
    }

    public void playMusicQuestion() {
        stopMusic();
        Platform.runLater(() -> {
            musicQuestion.play();
        });
    }

    public void playPostQuestion() {
        stopMusic();
        Platform.runLater(() -> sfxEndQuestion.play());
    }

    public void stopMusic()
    {
        this.musicMenu.stop();
        this.musicQuestion.stop();
        this.sfxEndQuestion.stop();
    }


    public boolean hasNext()
    {
        if (itrQuestions == null)
            return false;
        return itrQuestions.hasNext();
    }

    public QuestionBase getNext()
    {
        if (itrQuestions == null)
            return null;
        return (currentQuestion = itrQuestions.next());
    }

    public QuestionBase getCurrent()
    {
        return currentQuestion;
    }

    public static List<QuestionBase> getQuestionsListFromJson(JSONObject jsonQuestions) throws ClassNotFoundException {
        List<QuestionBase> list = new ArrayList<>();

        assert jsonQuestions != null;

        JSONArray arrayQuestions;
        arrayQuestions = jsonQuestions.getJSONArray("questions");

        for (int i = 0; i < arrayQuestions.length(); i++)
        {
            JSONObject jsonQuestion;
            jsonQuestion = (JSONObject) arrayQuestions.get(i);

            String questionType = jsonQuestion.getString("question_type");

            QuestionBase base;
            base = getQuestionObjectFromName(questionType);
            if (base != null) {
                base.loadJson(jsonQuestion);
                list.add(base);
            }
        }

        return list;
    }

    public static QuestionBase getQuestionObjectFromName(String name) {
        String clsStr = "com.igalblech.igalsquizserver.Questions." + name;
        try {
            Class cls = Class.forName(clsStr);
            return (QuestionBase) cls.getConstructor().newInstance();
        }
        catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("QuestionsSet {\n  questions: [");

        for (int i = 0; i < questionsList.size(); i++) {
            sb.append(questionsList.get(i).toString()); // Assuming QuestionBase has a toString method
            if (i < questionsList.size() - 1) {
                sb.append(",\n");
            }
        }
        sb.append("\n  ]\n}");
        return sb.toString();
    }

    public void setQuestionsList(List<QuestionBase> questionsList) {
        this.questionsList = questionsList;
        this.itrQuestions = questionsList.listIterator();
    }

    public void sendQuestionEnd() {
        serverSocket.sendQuestionEnd(this.getCurrent());
    }
}
