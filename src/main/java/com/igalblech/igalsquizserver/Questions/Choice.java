package com.igalblech.igalsquizserver.Questions;

import com.fasterxml.jackson.databind.JsonNode;
import com.igalblech.igalsquizserver.QuizApplication;
import javafx.scene.image.Image;
import javafx.scene.media.Media;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONArray;
import org.json.JSONObject;
import com.igalblech.igalsquizserver.Utils;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

@Getter
public class Choice {

    String text = "";
    boolean isRight = false;
    int order = -1;
    @Setter
    int[] color = new int[]{};

    Image image = null;
    Media sound= null;


    @Override
    public String toString() {
        return String.format("Choice { text: '%s', isTrue: %b }", text, isRight);
    }

    public void loadJson(JSONObject json) {
        if (json.has("text"))
            this.text = json.getString("text");
        if (json.has("is_right"))
            this.isRight = json.getBoolean("is_right");
        if (json.has("order"))
            this.order = json.getInt("order");

        String imageStr = null, soundStr = null;
        if (json.has("image"))
            imageStr =  json.getString("image");
        if (json.has("sound"))
         soundStr = json.getString("sound");

        if (json.has("color")) {
            JSONArray jsonColors = json.getJSONArray("json");
            color = new int[jsonColors.length()];
            for (int i = 0; i < jsonColors.length(); i++)
                color[i] = jsonColors.getInt(i);
        }

        if (imageStr != null && !imageStr.equals(""))
        {
            try {
                URL resource = QuizApplication.class.getResource(imageStr);
                this.image = new Image(resource.openStream());
            } catch (IllegalArgumentException | IOException | NullPointerException e) {
                System.out.println("Can't find " + imageStr);
            }
        }

        if (soundStr != null && !soundStr.equals("")) {
            final URL resource = QuizApplication.class.getResource(soundStr);
            if (resource != null)
                this.sound = new javafx.scene.media.Media(resource.toString());
            else
                System.out.println("Can't find " + soundStr);
        }
    }

    JSONObject toJson()
    {
        JSONObject out = new JSONObject();
        out.put("text", text);
        out.put("isRight", isRight);
        out.put("order", order);
        out.put("color", color);
        if (image != null) {
            out.put("image",  Utils.encodeImageToBase64(image));
        }

        return out;
    }
}