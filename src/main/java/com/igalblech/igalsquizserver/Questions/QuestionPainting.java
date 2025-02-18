package com.igalblech.igalsquizserver.Questions;

import javafx.fxml.FXML;
import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class QuestionPainting extends QuestionBase implements Cloneable {

    List<String> colors = new ArrayList<>();
    String backgroundColor = "rgb(0, 0, 0)";
    int w = 300;
    int h = 300;

    String image;

    public QuestionPainting() {
        super();
    }

    @Override
    public void loadJsonSub(@NonNull JSONObject object) {
        if (object.has("painting_background"))
            this.backgroundColor = object.getString("painting_background");

        if (object.has("painting_colors")) {
            JSONArray jsonArray = object.getJSONArray("painting_colors");
            for (int i = 0; i < jsonArray.length(); i++) {
                this.colors.add(jsonArray.getString(i));
            }
        }

        if (object.has("painting_w"))
            this.w = object.getInt("painting_w");

        if (object.has("painting_h"))
            this.h = object.getInt("painting_h");
    }

    @Override
    public void toJsonSub(@NonNull JSONObject object) {
        toJsonQuestionOnlySub(object);
    }

    @Override
    public void toJsonQuestionOnlySub(@NonNull JSONObject out) {
        out.put("paintingW", this.w);
        out.put("paintingH", this.h);
        out.put("paintingBackground", this.backgroundColor);
        out.put("paintingColors", this.colors);
    }

    @Override
    public Answer compareAnswer(@Nullable JSONObject jsonObject) {
        Answer out = new Answer();
        out.setExternalData(jsonObject);
        return out;
    }

    @Override
    public QuestionPainting clone() {
        try {
            QuestionPainting clone = (QuestionPainting) super.clone();
            clone.colors = new ArrayList<>(this.colors);
            clone.backgroundColor = this.backgroundColor;
            clone.w = this.w;
            clone.h = this.h;
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
