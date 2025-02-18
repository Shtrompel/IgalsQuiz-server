package com.igalblech.igalsquizserver.controllers;

import com.igalblech.igalsquizserver.InterfaceController;
import com.igalblech.igalsquizserver.QuizApplication;
import com.igalblech.igalsquizserver.UserData;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import lombok.Getter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SceneManager {

    private final Map<String, SceneData> scenes = new HashMap<>();
    @Getter
    private final Scene mainScene;

    @Getter
    Pane centerContainer;
    @Getter
    UserData userData;

    public Application application;

    @Getter
    private SceneData currentScene = null;

    @Getter
    public static class SceneData
    {
        private Parent parent = null;
        private InterfaceController controller = null;
    }

    public SceneManager(Application app, Scene mainScene)
    {
        this(app, mainScene, null);
    }

    public SceneManager(Application app, Scene mainScene, UserData userData)
    {
        this.application = app;
        this.mainScene = mainScene;
        this.userData = userData;
    }

    public void changeScene(String title) throws IllegalArgumentException{

        Platform.runLater(() -> {
            changeSceneThisThread(title);
        });
    }

    public void changeSceneThisThread(String title) throws IllegalArgumentException {

        System.out.println("Changed stage to " + title + " ...");

        if (currentScene != null)
        {
            currentScene.controller.onClose();
        }

        SceneData sceneData;
        if (!scenes.containsKey(title))
        {
            throw new IllegalArgumentException("Did not found controller " + title);
        }
        sceneData = this.scenes.get(title);
        this.currentScene = sceneData;

        this.centerContainer = (Pane) mainScene.lookup("#contentsContainer");
        this.centerContainer.getChildren().setAll(sceneData.parent);

        if (sceneData.controller != null)
            sceneData.controller.onShown();

        mainScene.lookup("#menuBar").setMouseTransparent(false);

        System.out.println("Changed stage to " + title + " done!");

    }

    public void addStage(String title, String path) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(
                QuizApplication.class.getResource("fxml/" + path));

        Parent parent = fxmlLoader.load();
        SceneData stageData = new SceneData();
        stageData.parent = parent;

        Object controller = fxmlLoader.getController();
        if (controller instanceof InterfaceController) {
            InterfaceController interfaceController;
            interfaceController = (InterfaceController)controller;
            interfaceController.setScene(this);
            interfaceController.setApp(application);
            stageData.controller = interfaceController;
        }

        if (title != null)
            scenes.put(title, stageData);
    }

    public InterfaceController getController(String title) throws ClassCastException, NullPointerException
    {
        return scenes.get(title).controller;
    }

    public void onKeyboardPressed(KeyEvent e) {

        for (SceneData data : scenes.values())
        {
            data.controller.onKeyboardButtonPressed(e);
        }

        if (e.getCode() == KeyCode.SPACE) {
            System.out.println("The Space key was pressed");
        }
    }

}
