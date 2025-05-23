package com.igalblech.igalsquizserver.controllers;

import javafx.application.Application;
import javafx.scene.input.KeyEvent;

public interface InterfaceController {

    void setScene(SceneManager scene);

    void setApp(Application app);

    void onShown();

    void onClose();

    void onKeyboardButtonPressed(KeyEvent e);

    void onSendQuestionEnd();

}
