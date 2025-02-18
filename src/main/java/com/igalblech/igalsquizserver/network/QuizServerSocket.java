package com.igalblech.igalsquizserver.network;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.igalblech.igalsquizserver.OnGetAnswer;
import com.igalblech.igalsquizserver.Questions.Answer;
import com.igalblech.igalsquizserver.Questions.QuestionBase;
import com.igalblech.igalsquizserver.SocketMessage;
import com.igalblech.igalsquizserver.controllers.OnPlayerName;
import javafx.application.Platform;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.databind.JsonNode;
import org.json.JSONObject;


import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class QuizServerSocket {

    @Getter
    private Set<SocketIOClient> connectedClients;

    private final SocketIOServer server;
    final ObjectMapper objectMapper = new ObjectMapper();

    @Getter
    private final Map<String, PlayerHandler> playerHandlers;

    @Setter
    private OnGetAnswer onGetAnswer = null;

    @Setter
    OnPlayerName onPlayerName;

    public QuizServerSocket(String address, int ipPort) {
        connectedClients = new HashSet<>();
        playerHandlers = new HashMap<>();

        Configuration config = new Configuration();
        config.setHostname(address);
        config.setPort(ipPort);
        config.setMaxFramePayloadLength(1048576);
        config.setMaxHttpContentLength(1048576);

        server = new SocketIOServer(config);



        // Register events

        server.addConnectListener(this::onConnected);
        server.addDisconnectListener(this::onDisconnected);

        server.addEventListener("message", JsonNode.class, (client, data, ackRequest) -> {

            SocketMessage message;
            try {
                message = objectMapper.treeToValue(data, SocketMessage.class);
            }
            catch (IllegalArgumentException | JsonProcessingException e)
            {
                e.printStackTrace();
                return;
            }

            System.out.println("Received data: " + message.toString());

            receiveSocketMessage(client, message);
        });

        /*server.addEventListener("message", SocketMessage.class, this::onMessage);

        server.addEventListener("connect_error", String.class, this::onError);
        server.addEventListener("connect_failed", String.class, this::onError);
        server.addEventListener("disconnect", String.class, this::onError);
         */
    }

    public void removedUnnamed()
    {
        playerHandlers.values().removeIf(player -> player.getName() == null || player.getName().isEmpty());
    }

    private void onDisconnected(SocketIOClient client) {
        System.out.println("Disconnected: " + client.getRemoteAddress());
    }

    private void onConnected(SocketIOClient client) {
        System.out.println("New connection: " + client.getRemoteAddress());
    }

    private void onError(SocketIOClient client, String string, AckRequest ackRequest) {
    }

    public void start() {
        server.start();
        System.out.println("Server started on port: " + server.getConfiguration().getPort());
    }

    public void stop() {
        server.stop();
        System.out.println("Server stopped.");
    }

    void receiveSocketMessage(SocketIOClient client, SocketMessage socketMessage)
    {
        // Get the type of the message
        System.out.println("Message in...");

        String type = socketMessage.getType();

        if (type.isEmpty())
        {
            System.out.println("Json Message has no \"type\" string! Aborting!");
            return;
        }

        // If it's the first message, it should be a "connect" message

        if (type.equals("connect"))
        {
            String id;
            if (socketMessage.getId().isEmpty())
                id = UUID.randomUUID().toString();
            else
                id = socketMessage.getId();

            if (!playerHandlers.containsKey(id))
            {
                connectedClients.add(client);

                PlayerHandler player = new PlayerHandler(client, id);
                playerHandlers.put(id, player);
            }

            SocketMessage msgOut;
            msgOut = new SocketMessage(id, "confirm_connect", "server", "");
            client.sendEvent("message", objectMapper.valueToTree(msgOut).toString());

            System.out.println("Out ID: " + id);
            return;
        }

        String id = socketMessage.getId();

        if (id.isEmpty())
        {
            System.out.println("Json Message has no \"id\" string! Aborting!");
            return;
        }


        PlayerHandler player;
        player = playerHandlers.get(id);

        switch (type)
        {
            case "set_name":
            {
                String name = socketMessage.getName();
                player.setName(name);
                System.out.println("Name Set To: " + name + " , " + (onPlayerName == null ? "null" : "not null"));
                {
                    if (onPlayerName != null)
                        Platform.runLater(() -> onPlayerName.nameEntered(name, player));
                }
                break;
            }
            case "disconnect":
            {
                playerHandlers.remove(socketMessage.getId());
                if (onPlayerName != null)
                    Platform.runLater(() -> onPlayerName.nameRemoved(player));
                break;
            }
            case "connect":
            {
                break;
            }
            case "send_answer":
            {
                onGetAnswer.onGetAnswer(id, socketMessage.getJsonData());
                break;
            }
            default:
                System.out.println("Message \"" + type + "\" was not found.");
        }
    }

    public void sendQuestionData() {
        SocketMessage socketMessage = new SocketMessage();
        socketMessage.setType("send_question");
        System.out.println(socketMessage.getJsonData());

        sendMessageToAll(socketMessage);
    }

    public void sendQuestionStart(QuestionBase current) {
        SocketMessage socketMessage = new SocketMessage();
        socketMessage.setType("question_start");
        socketMessage.setJsonData(current.toJson().toString());

        try {
            FileWriter myWriter = new FileWriter("filename.txt");
            myWriter.write(current.toJson().toString());
            myWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        sendMessageToAll(socketMessage);
    }

    public void sendEndGame(boolean temporarily) {
        JSONObject object = new JSONObject();
        object.put("temporarily", temporarily);

        SocketMessage socketMessage = new SocketMessage();
        socketMessage.setType("quiz_end");
        socketMessage.setJsonData(object.toString());

        if (!temporarily) {
            List<PlayerHandler> list;
            list = getSortedPlayerHandlers(playerHandlers);
            int i = 1;
            for (PlayerHandler handler : list) {
                object.put("totalPoints", handler.getPoints());
                object.put("level", i++);
                socketMessage.setJsonData(object.toString());
                sendMessageToClient(handler, socketMessage);
                handler.setAnswer(null);
            }
        }

    }

    public void sendQuestionEnd(QuestionBase current) {

        Map<String, PlayerHandler> players;
        players = this.getPlayerHandlers();
        for (PlayerHandler handler : players.values())
        {
            if (!handler.hasAnswer())
                handler.setAnswer(current.compareAnswer(null));
            Answer rightAnswer = handler.getAnswer();

            System.out.println("Answer 1: " + rightAnswer.toJson().toString());

            if (rightAnswer != null)
                handler.addPoints(rightAnswer.getPoints());
        }


        SocketMessage socketMessage = new SocketMessage();
        socketMessage.setType("question_end");

        List<PlayerHandler> list;
        list = getSortedPlayerHandlers(playerHandlers);

        int i=1;
        for (PlayerHandler handler : list) {
            JSONObject node;
            Answer trueAnswer = handler.getAnswer();
            if (trueAnswer == null)
                node = new Answer().toJson();
            else
                node = trueAnswer.toJson();
            node.put("totalPoints", handler.getPoints());
            node.put("level", i++);
            socketMessage.setJsonData(node.toString());

            sendMessageToClient(handler, socketMessage);
            handler.setAnswer(null);
        }
    }

    public void sendMessageToAll(SocketMessage message)
    {
        for (PlayerHandler handler : playerHandlers.values())
        {
            sendMessageToClient(handler, message);
        }
    }

    public void sendMessageToClient(PlayerHandler playerHandler, SocketMessage message)
    {
        SocketIOClient client = playerHandler.getWebSocket();
        message.setSource("server");
        message.setName(playerHandler.getUuid());
        message.setId(playerHandler.getUuid());
        client.sendEvent("message", objectMapper.valueToTree(message).toString());
    }

    static List<PlayerHandler> getSortedPlayerHandlers(Map<String, PlayerHandler> playerHandlers)
    {
        List<PlayerHandler> sortedPlayers =
                new ArrayList<>();
        for (PlayerHandler handler : playerHandlers.values())
        {
            int indexToAdd = 0;
            for (PlayerHandler current : sortedPlayers) {
                if (handler.getPoints() >= current.getPoints())
                    break;
                indexToAdd++;
            }
            sortedPlayers.add(indexToAdd, handler);
        }
        return sortedPlayers;
    }

    public void resetGame() {
        for (PlayerHandler v : playerHandlers.values())
        {
            v.reset();
        }
    }

    public void removePlayer(PlayerHandler handler) {
        playerHandlers.remove(handler.getUuid());
    }

    public void sendPunishment(PlayerHandler handler, String random) {

    }

    public void sendAward(PlayerHandler handler, String random) {

    }

/*
    void receiveJson(SocketIOClient conn, JsonNode jsonMsg)
    {

        if (!jsonMsg.has("id"))
        {
            System.out.println("Json Message has no \"id\" string! Aborting!");
            return;
        }

        if (!jsonMsg.has("type"))
        {
            System.out.println("Json Message has no \"type\" string! Aborting!");
            return;
        }

        String id = jsonMsg.getString("id");

        if (jsonMsg.getString("type").equals("connect"))
        {
            System.out.println("In ID: " + id);
            if (id.isEmpty())
            {
                id = UUID.randomUUID().toString();
            }

            JSONObject jsonOut = new JSONObject();
            jsonOut.put("id", id);
            jsonOut.put("type", "confirm_connect");

            conn.sendEvent("message", jsonOut.toString());
            System.out.println("Out ID: " + id);
        }

        PlayerHandler player;
        if (playerHandlers.containsKey(id))
        {
            player = playerHandlers.get(id);
        }
        else
        {
            player = new PlayerHandler(conn, id);
            playerHandlers.put(id, player);
        }

        String messageType = jsonMsg.getString("type");
        System.out.println("Message Type: " + messageType);
        switch (messageType)
        {
            case "set_name":
            {
                String name = jsonMsg.getString("name");
                player.setName(name);
                System.out.println("Name Set To: " + name + " , " + (onPlayerName == null ? "null" : "not null"));
                {
                    if (onPlayerName != null)
                        Platform.runLater(() -> onPlayerName.nameEntered(name, player));

                }
                break;
            }
            case "":
            {

            }
            default:
                System.out.println("Message \"" + messageType + "\" was not found.");
        }
    }
 */

}
