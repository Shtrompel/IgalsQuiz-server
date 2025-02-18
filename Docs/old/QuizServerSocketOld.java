package com.igalblech.igalsquizserver;

import lombok.Getter;

import java.io.*;
import java.lang.ref.WeakReference;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class QuizServerSocketOld {

    @Getter
    private final Map<String, PlayerHandler> playerHandler;
    private final ServerSocket serverSocket;
    GetClientsThread thread;

    QuizServerSocketOld(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        playerHandler = Collections.synchronizedMap(new HashMap<>());
    }

    public void start() {
        thread = new GetClientsThread(playerHandler, serverSocket);
        thread.start();
    }

    public void stopAll() {
        thread.interrupt();
        thread.onStop();
        for (var v : playerHandler.values())
        {
            v.interrupt();
            v.onStop();
        }
    }

    private static class GetClientsThread extends Thread {

        private final WeakReference<Map<String, PlayerHandler>> playerSockets;

        private final ServerSocket serverSocket;
        boolean keepReading = true;

        GetClientsThread(Map<String, PlayerHandler> playerSockets, ServerSocket serverSocket) {
            this.playerSockets = new WeakReference<>(playerSockets);
            this.serverSocket = serverSocket;
        }

        @Override
        public void run() {
            while (keepReading) {
                Socket socket;
                try {
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    System.out.println(e);
                    keepReading = false;
                    continue;
                }

                System.out.println("Socket Accepted!");

                // Get input and output streams
                DataInputStream inputStream;
                DataOutputStream outputStream;
                try {
                    inputStream = new DataInputStream(socket.getInputStream());
                    outputStream = new DataOutputStream(socket.getOutputStream());
                }
                catch (IOException e) {
                    System.out.println(e);
                    keepReading = false;
                    continue;
                }

                System.out.println("Got IO Streams");

                // Get id on the client, if not found generate one.
                String uuid;
                try {
                    uuid = requestId(inputStream, outputStream);
                } catch (IOException e) {
                    System.out.println(e);
                    keepReading = false;
                    continue;
                }

                System.out.println("Got client Id");

                // Make a handler for data io of player data
                PlayerHandler handler;
                handler = new PlayerHandler(
                        socket, inputStream, outputStream, uuid);
                // Run player io thread
                handler.start();
                // Add to map if does not exists
                if (!playerSockets.get().containsKey(uuid))
                {
                    playerSockets.get().put(uuid, handler);
                }

                System.out.println("Start io thread");

                // Start player io thread
                handler.start();
            }
        }

        String requestId(
                DataInputStream inputStream,
                DataOutputStream outputStream) throws IOException {
            String id = inputStream.readUTF();
            if (id.isEmpty())
            {
                id = UUID.randomUUID().toString();
            }
            outputStream.writeUTF(id);
            return id;
        }

        void onStop()
        {

        }
    }

    private static class PlayerHandler extends Thread {

        Socket socket;
        String uuid;
        String name;
        DataInputStream inputStream;
        DataOutputStream outputStream;

        GameMode gameMode;

        public PlayerHandler(
                Socket socket,
                DataInputStream inputStream,
                DataOutputStream outputStream,
                String uuid)
        {
            this.socket = socket;
            this.inputStream = inputStream;
            this.outputStream = outputStream;
            this.uuid = uuid;
            this.name = uuid;
        }

        @Override
        public void run() {



            // Get player name
            try {
                String name = inputStream.readUTF();
            } catch (IOException e) {

            }
        }

        void onStop()
        {

        }

    }
}
