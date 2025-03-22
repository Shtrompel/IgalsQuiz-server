package com.igalblech.igalsquizserver.controllers;

import com.igalblech.igalsquizserver.SharedSessionData;
import com.igalblech.igalsquizserver.network.PlayerHandler;
import com.igalblech.igalsquizserver.network.QuizServerSocket;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.*;
import javafx.stage.Window;

import java.net.UnknownHostException;
import java.util.ArrayList;

public class ListPlayersController {

    @FXML
    GridPane gridPanePlayers;
    @FXML
    VBox boxPlayers;

    SharedSessionData sessionData = null;
    SceneManager manager;

    public void initialize()
    {
    }

    public void init(SceneManager manager) {
        this.manager = manager;
        this.sessionData = (SharedSessionData) manager.getUserData();

        var players = new ArrayList<>(sessionData.getServerSocket().getPlayerHandlers().values());
        int c = gridPanePlayers.getColumnCount();
        int r = gridPanePlayers.getRowCount();
        int s = gridPanePlayers.getRowCount() * gridPanePlayers.getColumnCount();

        if (players.size() / c > r)
        {
            for (int i = 0; i <= players.size() / c - r; i++)
            {
                RowConstraints con = new RowConstraints();
                gridPanePlayers.getRowConstraints().add(con);
            }
        }

        c = gridPanePlayers.getColumnCount();
        gridPanePlayers.setAlignment(Pos.CENTER);
        updateLayout(
                gridPanePlayers.getScene().getWindow().getWidth(),
                gridPanePlayers.getScene().getWindow().getHeight());


        int v = 0;
        for (var handler : players) {

            HBox boxPlayer = new HBox();
            boxPlayer.setSpacing(10);
            boxPlayer.setAlignment(Pos.CENTER);

            Region spaceLeft = new Region();
            Region spaceRight = new Region();
            HBox.setHgrow(spaceLeft, Priority.ALWAYS);
            HBox.setHgrow(spaceRight, Priority.ALWAYS);

            Label labelName = new Label(handler.getName());
            Label labelColons = new Label(":");
            Label labelPoints = new Label(String.format("%d Points", handler.getPoints()));

            labelName.setMaxWidth(Double.MAX_VALUE);
            labelName.setMinWidth(100);
            labelName.setEllipsisString("");

            labelPoints.setMaxWidth(Double.MAX_VALUE);
            labelPoints.setMinWidth(100);
            labelPoints.setEllipsisString("");

            labelName.setPadding(new Insets(0, 20, 0, 20));
            labelPoints.setPadding(new Insets(0, 20, 0, 20));

            boxPlayer.getChildren().addAll(labelName, spaceLeft, labelColons, spaceRight, labelPoints);
            boxPlayer.setUserData(handler.getUuid());

            ContextMenu contextMenu = createPlayerContextMenu(handler, labelPoints);
            boxPlayer.setOnMouseClicked(event -> {
                contextMenu.show(boxPlayer, event.getScreenX(), event.getScreenY());
            });

            gridPanePlayers.add(boxPlayer, v % c, v / c);
            GridPane.setHgrow(boxPlayer, Priority.ALWAYS);
            GridPane.setVgrow(boxPlayer, Priority.ALWAYS);
            v++;
        }

        Window window = boxPlayers.getScene().getWindow();

        window.widthProperty().addListener((observable, oldValue, newValue) -> {
            updateLayout(newValue.doubleValue(), height);
        });

        window.heightProperty().addListener((observable, oldValue, newValue) -> {
            updateLayout(width, newValue.doubleValue());
        });


    }

    double width = 0;
    double height = 0;

    void updateLayout(double width, double height)
    {
        this.width = width;
        this.height = height;

        System.out.println(width  + ", " + height);

        int c = gridPanePlayers.getColumnCount();
        int r = gridPanePlayers.getRowCount();

        for (var colCon : gridPanePlayers.getColumnConstraints())
        {
            double w = width / c;
            colCon.setMinWidth(w);
            colCon.setPrefWidth(w);
            colCon.setMaxWidth(50);
        }

        for (var rowsCon : gridPanePlayers.getRowConstraints())
        {
            double h = height / r;
            rowsCon.setMinHeight(h);
            rowsCon.setPrefHeight(h);
            rowsCon.setMaxHeight(50);
        }

        gridPanePlayers.layout();
    }

    ContextMenu createPlayerContextMenu(PlayerHandler handler, Label labelPoints)
    {
        QuizServerSocket socket;
        socket = ((SharedSessionData)manager.getUserData()).getServerSocket();


        ContextMenu contextMenu = new ContextMenu();
        if (handler != null) {
            MenuItem itemKick = new MenuItem("Kick");
            itemKick.setOnAction(e -> {
                nameRemoved(handler);
                socket.removePlayer(handler);
            });

            MenuItem itemAdd10 = new MenuItem("Add 10 Points");
            itemAdd10.setOnAction(e -> {
                handler.addPoints(10);
                labelPoints.setText(handler.getPoints() + " Points");
            });

            MenuItem itemAdd100 = new MenuItem("Add 100 Points");
            itemAdd100.setOnAction(e -> {
                handler.addPoints(100);
                labelPoints.setText(handler.getPoints() + " Points");
            });

            MenuItem itemSub10 = new MenuItem("Remove 10 Points");
            itemSub10.setOnAction(e -> {
                handler.addPoints(-10);
                labelPoints.setText(handler.getPoints() + " Points");
            });

            MenuItem itemSub100 = new MenuItem("Remove 100 Points");
            itemSub100.setOnAction(e -> {
                handler.addPoints(-100);
                labelPoints.setText(handler.getPoints() + " Points");
            });

            MenuItem itemPunish = new MenuItem("Random Punishment");
            itemPunish.setOnAction(e -> sessionData.getServerSocket().sendPunishment(handler, "RANDOM"));

            MenuItem itemAward = new MenuItem("Random Award");
            itemAward.setOnAction(e -> sessionData.getServerSocket().sendAward(handler, "RANDOM"));

            contextMenu.getItems().addAll(
                    itemKick,
                    new SeparatorMenuItem(),
                    itemAdd10,
                    itemAdd100,
                    new SeparatorMenuItem(),
                    itemSub10,
                    itemSub100,
                    new SeparatorMenuItem(),
                    itemPunish,
                    itemAward);
        }
        return contextMenu;
    }

    public void nameRemoved(PlayerHandler handler) {
        System.out.println(handler.getUuid());
        for (Node node : gridPanePlayers.getChildren()) {
            Object obj = node.getUserData();
            System.out.print(obj + " ");
            if (obj == null || !(obj instanceof String))
                continue;

            String id = (String) node.getUserData();
            if (id.equals(handler.getUuid()) && node instanceof HBox)
            {
                System.out.print("yees");
                gridPanePlayers.getChildren().remove(node);
                break;
            }
        }
        System.out.println();

        rearrangeGrid();
    }

    Node[] clearGrid()
    {
        // Temporarily hold all current nodes
        Node[] nodes = gridPanePlayers.getChildren().toArray(new Node[0]);

        // Clear the grid
        gridPanePlayers.getChildren().clear();

        gridPanePlayers.getChildren().add(nodes[0]);

        return nodes;
    }

    private void rearrangeGrid() {
        int columnCount = gridPanePlayers.getColumnCount();
        int row = 0;
        int col = 0;

        Node[] nodes = clearGrid();

        // Re-add each node in order without gaps
        for (int i = 1; i < nodes.length; i++)
        {
            Node node = nodes[i];
            gridPanePlayers.add(node, col, row);
            col++;
            if (col >= columnCount) {
                col = 0;
                row++;
            }
        }
    }

}
