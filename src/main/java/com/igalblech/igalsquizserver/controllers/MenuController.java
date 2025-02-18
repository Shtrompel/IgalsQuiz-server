package com.igalblech.igalsquizserver.controllers;

import com.igalblech.igalsquizserver.*;
import com.igalblech.igalsquizserver.Questions.QuestionBase;
import com.igalblech.igalsquizserver.network.PlayerHandler;
import com.igalblech.igalsquizserver.network.QuizServerSocket;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

public class MenuController implements InterfaceController, OnPlayerName {

    @FXML
    Button buttonLoadQuestions;
    @FXML
    Button buttonExit;
    @FXML
    Text textMenuIp;
    @FXML
    GridPane gridUsers;

    SceneManager stageManager;
    QuizApplication application;

    public void initialize() throws UnknownHostException {
        System.out.println("MenuController initialize()");
        System.out.println(Inet4Address.getLocalHost().getHostAddress());
        textMenuIp.setText("IP is " + QuizApplication.IP_ADDRESS + ":" + QuizApplication.IP_WEB_PORT);
    }

    @FXML
    void onLoadQuestionsButtonPressed() {

        // Remove all unnamed sockets
        application.getServerSocket().removedUnnamed();

        SharedSessionData questionsSet = (SharedSessionData) stageManager.getUserData();

        // One FileChooser

        Properties properties = ((SharedSessionData) stageManager.getUserData()).getProperties();
        final FileChooser fileChooser = new FileChooser();
        String lastPathStr = properties.getProperty("lastDirectory");

        if (lastPathStr != null) {
            String lastDirPath = URLDecoder.decode(lastPathStr);
            File lastDirectory = new File(lastDirPath);
            if (lastDirectory.exists()) {
                fileChooser.setInitialDirectory(lastDirectory);
            }
        }

        FileChooser.ExtensionFilter extensionFilter =
                new FileChooser.ExtensionFilter("Igal's Cool Quiz File", "*.txt", "*.json");
        fileChooser.getExtensionFilters().add(extensionFilter);

        File file = null;
        if (properties.getProperty("forceLoad", "false").equals("true")) {
            file = new File(java.net.URLDecoder.decode(
                    properties.getProperty("lastFile")));
        }
        if (file == null) {
            file = fileChooser.showOpenDialog(stageManager.getMainScene().getWindow());
        }

        if (file != null) {
            try {
                properties.setProperty(
                        "lastDirectory",
                        URLEncoder.encode(file.getParentFile().getAbsolutePath())
                );
                properties.setProperty(
                        "lastFile",
                        URLEncoder.encode(file.getAbsolutePath())
                );

                // Require the file questions
                List<QuestionBase> questions;
                questions = fileToQuestions(file);
                questionsSet.setQuestionsList(questions);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                return;
            }
        }

        stageManager.changeScene("QUESTION_INTRO");

        /*
        boolean a = true;
        if (a) {
            QuestionController ic =
                    (QuestionController) stageManager.getController("QUESTION");
            assert ic != null;
            if (sharedSessionData.hasNext())
                return;
            ic.applyQuestion(sharedSessionData.getNext());
            stageManager.changeStage("QUESTION");
        }
        else
        {
            stageManager.changeStage("QUESTION_TRANSITION");
        }
        */
    }

    private List<QuestionBase> fileToQuestions(File file) throws IOException, ClassNotFoundException {
        String content;
        content = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
        JSONObject jsonObject = new JSONObject(content);
        SharedSessionData questionsSet = (SharedSessionData)stageManager.getUserData();

        //questionsSet.setQuestionsList(
        return SharedSessionData.getQuestionsListFromJson(jsonObject);
    }

    @FXML
    void onExitButtonPressed()
    {
        Platform.exit();
    }

    @Override
    public void setScene(SceneManager stageManager) {
        this.stageManager = stageManager;
    }

    @Override
    public void setApp(Application app) {
        assert app != null;
        this.application = (QuizApplication) app;
    }

    @Override
    public void onShown() {
        //todo check if works

        // onPlayerName as defined in this class with run inside the QuizServerSocket class
        SharedSessionData data;
        data = (SharedSessionData)stageManager.getUserData();
        data.getServerSocket().setOnPlayerName(this);

        for (int i = 0; i < 0; i++)
        {
            PlayerHandler handler = new PlayerHandler();
            handler.setName("name" + i);
            handler.setUuid("uuid" + i);
            data.getServerSocket().getPlayerHandlers().put(handler.getUuid(), handler);
        }

        clearGrid();
        for (PlayerHandler handler : data.getServerSocket().getPlayerHandlers().values())
        {
            nameEntered(handler.getName(), handler);
        }

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

    ContextMenu createPlayerContextMenu(PlayerHandler handler)
    {
        QuizServerSocket socket;
        socket = ((SharedSessionData)stageManager.getUserData()).getServerSocket();

        ContextMenu contextMenu = new ContextMenu();
        if (handler != null) {
            // Add menu items
            MenuItem itemKick = new MenuItem("Kick");
            // Set actions for menu items
            itemKick.setOnAction(e -> {
                nameRemoved(handler);
                socket.removePlayer(handler);
            });
            // Add items to the menu
            contextMenu.getItems().addAll(itemKick);
        }
        return contextMenu;
    }

    @Override
    public void nameEntered(String name, PlayerHandler handler) {

        ContextMenu contextMenu = createPlayerContextMenu(handler);

        for (Node node : gridUsers.getChildren())
        {
            Object obj = node.getUserData();
            if (obj == null || !(obj instanceof String))
                continue;
            String id = (String) node.getUserData();
            if ((handler != null && id.equals(handler.getUuid())) && node instanceof Label)
            {
                Label label = (Label)node;
                label.setText(name);
                return;
            }
        }

        Label label = new Label(name);
        if (handler != null)
            label.setUserData(handler.getUuid());

        label.setOnMouseClicked(event -> {
            contextMenu.show(label, event.getScreenX(), event.getScreenY());
        });

        int c = gridUsers.getChildren().size() - 1;
        gridUsers.add(
                label,
                c % gridUsers.getColumnCount(),
                c / gridUsers.getColumnCount());
    }

    @Override
    public void nameRemoved(PlayerHandler handler) {
        for (Node node : gridUsers.getChildren()) {
            Object obj = node.getUserData();
            if (obj == null || !(obj instanceof String))
                continue;

            String id = (String) node.getUserData();
            if (id.equals(handler.getUuid()) && node instanceof Label)
            {
                gridUsers.getChildren().remove(node);
                break;
            }
        }

        rearrangeGrid();
    }

    Node[] clearGrid()
    {
        // Temporarily hold all current nodes
        Node[] nodes = gridUsers.getChildren().toArray(new Node[0]);

        // Clear the grid
        gridUsers.getChildren().clear();

        gridUsers.getChildren().add(nodes[0]);

        return nodes;
    }

    private void rearrangeGrid() {
        int columnCount = gridUsers.getColumnCount();
        int row = 0;
        int col = 0;

        Node[] nodes = clearGrid();

        // Re-add each node in order without gaps
        for (int i = 1; i < nodes.length; i++)
        {
            Node node = nodes[i];
            gridUsers.add(node, col, row);
            col++;
            if (col >= columnCount) {
                col = 0;
                row++;
            }
        }
    }


}
