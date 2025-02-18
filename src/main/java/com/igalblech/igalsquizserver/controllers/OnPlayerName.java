package com.igalblech.igalsquizserver.controllers;

import com.igalblech.igalsquizserver.network.PlayerHandler;

public interface OnPlayerName {

    void nameEntered(String name, PlayerHandler handler);

    void nameRemoved(PlayerHandler handler);

}
