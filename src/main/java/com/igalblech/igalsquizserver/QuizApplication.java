package com.igalblech.igalsquizserver;


import com.igalblech.igalsquizserver.controllers.MainController;
import com.igalblech.igalsquizserver.controllers.SceneManager;
import com.igalblech.igalsquizserver.network.QuizServerSocket;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.Getter;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Properties;


public class QuizApplication extends Application {

    SceneManager manager;
    Stage stage;
    public static final String IP_ADDRESS = "192.168.68.55";

    public static final  int IP_PORT = 5205;
    public static final  int IP_WEB_PORT = 4200;

    @Getter
    private QuizServerSocket serverSocket;
    private static final String CONFIG_FILE = "config.properties";
    private Properties properties;

    public static String questionTypeToSceneName(String questionType)
    {
        String sceneName = "";
        switch (questionType)
        {
            case "QuestionChoice":
            case "QuestionOrder":
                sceneName = "QUESTION";
                break;
            case "QuestionPainting":
                sceneName = "QUESTION_PAINTING";
                break;
            case "QuestionMinigame":
                sceneName = "QUESTION_GAME";
                break;
        }
        return sceneName;
    }

    @Override
    public void start(Stage stage) throws IOException {

        // Initialize Properties

        this.properties = new Properties();

        try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
            properties.load(in);
        } catch (IOException e) {
            System.out.println("Config file not found, starting with default settings.");
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
                properties.store(out, "FileChooser Configuration");
            } catch (IOException e) {
                System.out.println("Error saving config file: " + e.getMessage());
            }
        }));

        // Initialize Sockets

        try {
            serverSocket = new QuizServerSocket(IP_ADDRESS, IP_PORT);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }

        try {
            //serverSocket.setWebSocketFactory(SSLHandler.getContext());
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }

        serverSocket.start();

        // Initialize Gui

        FXMLLoader loader = new FXMLLoader(
                QuizApplication.class.getResource("fxml/MainLayout.fxml"));
        Parent root = loader.load();
        Scene mainScene = new Scene(root);

        mainScene.setOnKeyPressed(e -> {
            manager.onKeyboardPressed(e);
        });

        stage.setScene(mainScene);

        // Initialize Manager

        SharedSessionData userData = new SharedSessionData(serverSocket, properties);

        manager = new SceneManager(this, mainScene, userData);
        manager.addStage("MENU", "Menu.fxml");
        manager.addStage("QUESTION", "Question.fxml");
        manager.addStage("QUESTION_TRANSITION", "QuestionTransition.fxml");
        manager.addStage("QUESTION_INTRO", "QuestionIntro.fxml");
        manager.addStage("QUESTION_GAME", "QuestionGame.fxml");
        manager.addStage("QUESTION_PAINTING", "QuestionPainting.fxml");
        manager.addStage("QUIZ_END", "QuizEnd.fxml");


        userData.setOnGetSceneManager(() -> manager);
        this.stage = stage;
        ((MainController)loader.getController()).setApp(manager.application);
        ((MainController)loader.getController()).setScene(manager);

        // Initialize Manager Finishing

        manager.changeScene("MENU");

        stage.setOnCloseRequest(t -> {
            Platform.exit();
            System.exit(0);
        });
        stage.show();
    }

    public static void main(String[] args) {
        // setOnCrashHandler();
        launch();
    }

    static void setOnCrashHandler()
    {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                Calendar cal = Calendar.getInstance();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");

                String filename = "crashlogs/"+sdf.format(cal.getTime())+".txt";

                PrintStream writer;
                try {
                    writer = new PrintStream(filename, "UTF-8");
                    writer.println(e.getClass() + ": " + e.getMessage());
                    for (int i = 0; i < e.getStackTrace().length; i++) {
                        writer.println(e.getStackTrace()[i].toString());
                    }

                } catch (FileNotFoundException | UnsupportedEncodingException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }

            }
        });
    }
}