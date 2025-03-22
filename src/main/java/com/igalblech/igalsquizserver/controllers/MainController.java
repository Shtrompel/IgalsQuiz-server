package com.igalblech.igalsquizserver.controllers;

import com.igalblech.igalsquizserver.QuizApplication;
import com.igalblech.igalsquizserver.SharedSessionData;
import com.igalblech.igalsquizserver.network.QuizServerSocket;
import javafx.application.Application;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.beans.value.ChangeListener;

public class MainController  implements InterfaceController {

    @FXML
    MenuItem menuItemToMenu;
    @FXML
    MenuItem menuItemClose;

    SceneManager stageManager;
    Application app;

    public MainController()
    {
        System.out.println("Created: " + this);
    }

    public void onMenuItemClose()
    {
        QuizServerSocket serverSocket;
        serverSocket = ((SharedSessionData)stageManager.getUserData()).getServerSocket();
        serverSocket.sendEndGame(false);
        serverSocket.resetGame();

        System.exit(0);
    }

    public void onMenuItemToMenu()
    {
        QuizServerSocket serverSocket;
        serverSocket = ((SharedSessionData)stageManager.getUserData()).getServerSocket();
        serverSocket.sendEndGame(true);
        serverSocket.resetGame();

        this.stageManager.changeScene("MENU");
    }

    public void onMenuItemListPlayers() throws Exception {
        showListPlayersWindow();
    }

    @Override
    public void setScene(SceneManager stageManager) {
        System.out.println("setScene");
        assert stageManager != null;
        this.stageManager = stageManager;
    }

    public void initialize()
    {
    }

    @Override
    public void setApp(Application app) {
        System.out.println("setApp");
        assert app != null;
        this.app = app;
    }

    @Override
    public void onShown() {
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

    private void showListPlayersWindow() throws Exception {
        Stage extraStage = new Stage();
        extraStage.setTitle("Players");
        extraStage.setUserData(stageManager);

        // Load FXML if you have a separate layout
        FXMLLoader loader = new FXMLLoader(
                QuizApplication.getFileURL("fxml/ListPlayers.fxml")
        );


        VBox extraRoot = loader.load();
        Scene extraScene = new Scene(extraRoot, 640, 400);

        extraStage.setScene(extraScene);
        extraStage.initModality(Modality.WINDOW_MODAL); // Blocks input to other windows until closed
        extraStage.show();

        extraStage.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                // Close the window when it loses focus
                extraStage.close();
            }
        });

        ((ListPlayersController)loader.getController()).init(stageManager);
    }
}
