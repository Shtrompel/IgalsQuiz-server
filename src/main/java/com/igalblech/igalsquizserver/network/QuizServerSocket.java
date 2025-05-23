package com.igalblech.igalsquizserver.network;

import com.corundumstudio.socketio.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.igalblech.igalsquizserver.Questions.Answer;
import com.igalblech.igalsquizserver.Questions.QuestionBase;
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

        // Enable CORS for specific origins
        //config.setOrigin("http://localhost:5205"); // Allow requests from your frontend

        // Enable both WebSocket and polling transports
        //config.setTransports(Transport.WEBSOCKET, Transport.POLLING);
        //config.setContext("/socket.io/");

        server = new SocketIOServer(config);

        // Register events
        server.addConnectListener(this::onConnected);
        server.addDisconnectListener(this::onDisconnected);

        server.addEventListener("message", JsonNode.class, (client, data, ackRequest) -> {

            SocketMessage message;
            try {
                message = objectMapper.treeToValue(data, SocketMessage.class);
            } catch (IllegalArgumentException | JsonProcessingException e) {
                e.printStackTrace();
                return;
            }

            System.out.println("Received data: " + message.toString());

            receiveSocketMessage(client, message);
        });

        server.addEventListener("connect_error", String.class, this::onError);
        server.addEventListener("connect_failed", String.class, this::onError);
        server.addEventListener("disconnect", String.class, this::onError);
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
        System.out.println("Error: " + string);
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
            else
            {
                PlayerHandler player = playerHandlers.get(socketMessage.getId());
                player.setActive(true);
                player.setWebSocket(client);
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
                //playerHandlers.remove(socketMessage.getId());
                //if (onPlayerName != null)
                //    Platform.runLater(() -> onPlayerName.nameRemoved(player));
                PlayerHandler playerHandler = playerHandlers.get(socketMessage.getId());
                if (playerHandler != null)
                    playerHandlers.get(socketMessage.getId()).setActive(false);
                else
                    playerHandlers.remove(socketMessage.getId());
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

    public void sendQuestionData(int transitionTime) {
        SocketMessage socketMessage = new SocketMessage();
        socketMessage.setType("send_question");

        JSONObject object = new JSONObject();
        object.put("transitionTime", transitionTime);
        socketMessage.setJsonData(object.toString());

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

                System.out.println("QuizServerSocket.sendEndGame handler.setAnswer(null)");
                // handler.setAnswer(null);
            }
        }
    }

    public void sendPunishment(PlayerHandler handler, String random) {
        SocketMessage socketMessage = new SocketMessage();
        socketMessage.setType("punishment");
        sendMessageToClient(handler, socketMessage);
    }

    public void sendAward(PlayerHandler handler, String random) {
        SocketMessage socketMessage = new SocketMessage();
        socketMessage.setType("award");
        sendMessageToClient(handler, socketMessage);
    }

    public void sendQuestionEnd(QuestionBase current) {

        Map<String, PlayerHandler> players;
        players = this.getPlayerHandlers();
        for (PlayerHandler handler : players.values())
        {
            if (!handler.hasAnswer())
                handler.setAnswer(current.compareAnswer(null));
            Answer rightAnswer = handler.getAnswer();
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
            if (trueAnswer == null) {
                node = current.getDefaultAnswer().toJson();
            }
            else
                node = trueAnswer.toJson();
            node.put("totalPoints", handler.getPoints());
            node.put("level", i++);
            socketMessage.setJsonData(node.toString());

            sendMessageToClient(handler, socketMessage);
        }
    }

    public void sendMessageToAll(SocketMessage message)
    {
        for (PlayerHandler handler : playerHandlers.values())
        {
            System.out.println("QuizServerSocket.sendMessageToAll: " + handler.toString());
            System.out.println("QuizServerSocket.sendMessageToAll: " + message.toString());

            sendMessageToClient(handler, message);
        }
    }

    public void sendMessageToClient(PlayerHandler playerHandler, SocketMessage message)
    {
        SocketIOClient client = playerHandler.getWebSocket();
        message.setSource("server");
        message.setName(playerHandler.getUuid());
        message.setId(playerHandler.getUuid());
        if (client != null)
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

}
