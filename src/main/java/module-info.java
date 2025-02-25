
module com.igalblech.igalsquizserver {
    requires javafx.controls;
    requires javafx.fxml;
    requires static lombok;
    requires org.json;
    requires org.java_websocket;
    requires java.desktop;
    requires javafx.media;
    requires javafx.web;
    requires java.xml.bind;
    requires org.jetbrains.annotations;
    requires netty.socketio;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;

    opens com.igalblech.igalsquizserver to javafx.fxml, com.fasterxml.jackson.databind;
    exports com.igalblech.igalsquizserver;
    exports com.igalblech.igalsquizserver.Questions;
    opens com.igalblech.igalsquizserver.Questions to javafx.fxml;
    exports com.igalblech.igalsquizserver.controllers;
    exports com.igalblech.igalsquizserver.network;
    opens com.igalblech.igalsquizserver.controllers to com.fasterxml.jackson.databind, javafx.fxml;
    opens com.igalblech.igalsquizserver.network to com.fasterxml.jackson.databind, javafx.fxml;

}