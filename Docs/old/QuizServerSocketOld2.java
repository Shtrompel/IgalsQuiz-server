package com.igalblech.igalsquizserver;

import lombok.Getter;
import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.exceptions.InvalidDataException;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshakeBuilder;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class QuizServerSocketOld2 extends WebSocketServer {

    @Getter
    private Map<String, PlayerHandler> playerHandler;
    private boolean keepReading;

    public QuizServerSocketOld2(int ipPort) {
        super(new InetSocketAddress(ipPort));

        this.playerHandler = Collections.synchronizedMap(new HashMap<>());
        this.keepReading = true;
    }

    @Override
    public ServerHandshakeBuilder onWebsocketHandshakeReceivedAsServer(WebSocket conn, Draft draft, ClientHandshake request) throws InvalidDataException {
        ServerHandshakeBuilder response = super.onWebsocketHandshakeReceivedAsServer(conn, draft, request);

        response.put("Access-Control-Allow-Origin", "*");
        response.put("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        response.put("Access-Control-Allow-Headers", "Content-Type");

        return response;
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {

        System.out.println("New connection from client: " + webSocket.getRemoteSocketAddress());

        /*
        while (keepReading) {
            System.out.println("New connection: " + webSocket.getRemoteSocketAddress());

            playerHandler.get("clientId");


    
            // Generate a new UUID for the player or use a provided one
            String uuid = UUID.randomUUID().toString();
    
            // Create a handler for the new player
            PlayerHandler handler = new PlayerHandler(webSocket, uuid);
            playerHandler.put(uuid, handler);
    
            // Send the UUID back to the client
            webSocket.send(uuid);
        }
         */
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {

    }

    @Override
    public void onMessage(WebSocket webSocket, String s) {
        System.out.println(s);
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {

    }

    @Override
    public void onStart() {

    }

    public static class PlayerHandler
    {

        public PlayerHandler(WebSocket webSocket, String uuid) {
        }
    }
}
