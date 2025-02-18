package com.igalblech.igalsquizserver.Questions;

import com.fasterxml.jackson.databind.JsonNode;
import com.igalblech.igalsquizserver.QuizApplication;
import javafx.scene.image.Image;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;

@Getter
@Setter
@ToString
public abstract class QuestionBase implements QuestionAbstract
{

    protected String questionType;
    protected String title, description;

    protected boolean randomize;
    protected int points;
    protected int timeLimit;

    Image image;
    javafx.scene.media.Media sound;

    QuestionBase()
    {
        this.points = 0;
        this.timeLimit = 0;
    }

    public JSONObject toJson()
    {
        JSONObject out = new JSONObject();

        out.put("questionType", questionType);
        out.put("title", title);
        out.put("description", description);

        out.put("randomize", randomize);
        out.put("points", points);
        out.put("timeLimit", timeLimit);

        toJsonSub(out);

        return out;
    }

    public JSONObject toJsonQuestionOnly()
    {
        JSONObject out = new JSONObject();

        out.put("questionType", questionType);
        out.put("title", title);
        out.put("description", description);

        out.put("randomize", randomize);
        out.put("points", points);
        out.put("timeLimit", timeLimit);

        toJsonQuestionOnlySub(out);

        return out;
    }

    public void loadJson(JSONObject json) throws JSONException
    {
        String imageStr = null, soundStr = null;

        questionType = json.getString("question_type");
        if (json.has("title"))
            title = json.getString("title");
        if (json.has("description"))
            description = json.getString("description");

        if (json.has("randomize"))
            randomize = json.getBoolean("randomize");
        if (json.has("points"))
            points = json.getInt("points");
        if (json.has("time_limit"))
            timeLimit = json.getInt("time_limit");

        if (json.has("image"))
            imageStr = json.getString("image");
        if (json.has("sound"))
            soundStr = json.getString("sound");

        if (imageStr != null && !imageStr.isEmpty())
        {
            try {
                URL resource = QuizApplication.class.getResource(imageStr);
                assert resource != null;
                this.image = new Image(resource.openStream());
            } catch (IllegalArgumentException | IOException | NullPointerException e) {
                System.out.println("Can't find " + imageStr);
            }
        }

        if (soundStr != null && !soundStr.isEmpty()) {
            final URL resource = QuizApplication.class.getResource(soundStr);
            if (resource != null)
                this.sound = new javafx.scene.media.Media(resource.toString());
            else
                System.out.println("Can't find " + soundStr);
        }

        loadJsonSub(json);
    }
}