package com.igalblech.igalsquizserver.Questions;

import com.igalblech.igalsquizserver.QuizApplication;
import com.igalblech.igalsquizserver.utils.Utils;
import javafx.scene.image.Image;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

public class QuestionMatchPairs extends QuestionBase implements Cloneable {

    @Getter @Setter
    public static class ChoiceNode
    {
        int id;
        String text = "";
        Image img = null;
        ChoiceNode(int id, String text, Image img)
        {
            this.id = id;
            this.text = text;
            this.img = img;
        }

        public static ChoiceNode fromJson(int id, JSONObject jsonObject)
        {
            javafx.scene.image.Image image = null;
            String text = "";

            if (jsonObject.has("text")) {
                text = jsonObject.getString("text");
            }

            if (jsonObject.has("image")) {
                String imageStr = jsonObject.getString("image");
                try {
                    URL resource = QuizApplication.getFileURL(imageStr);
                    image = new javafx.scene.image.Image(resource.openStream());
                } catch (IllegalArgumentException | IOException | NullPointerException e) {
                    System.out.println("Can't find " + imageStr);
                }
            }

            return new ChoiceNode(id, text, image);
        }
    }

    @Getter
    private Map<Integer, ChoiceNode> items = new TreeMap<>();
    @Getter
    private Map<Integer, ChoiceNode> targets = new TreeMap<>();

    public QuestionMatchPairs() {
        super();
    }

    @Override
    public Object clone() {
        final QuestionMatchPairs clone;
        try {
            clone = (QuestionMatchPairs) super.clone();
        }
        catch (CloneNotSupportedException ex) {
            throw new RuntimeException("superclass messed up", ex);
        }
        clone.items = new TreeMap<>(this.items);
        clone.targets = new TreeMap<>(this.targets);
        return clone;
    }

    @Override
    public void loadJsonSub(JSONObject object) throws JSONException {

        System.out.println(object.toString());

        if (!object.has("pair_answers"))
        {
            return;
        }

        JSONArray jsonAnswers = object.getJSONArray("pair_answers");
        int counter = 0;
        Random random = new Random(0);
        for (Object pair : jsonAnswers) {
            JSONObject jsonPair = (JSONObject) pair;
            ChoiceNode nodeItem = ChoiceNode.fromJson(counter, jsonPair.getJSONObject("item"));
            ChoiceNode nodeTarget = ChoiceNode.fromJson(counter, jsonPair.getJSONObject("target"));
            items.put(generateUniqueRandomKey(items, random), nodeItem);
            targets.put(generateUniqueRandomKey(targets, random), nodeTarget);
            counter++;
        }
    }

    @Override
    public void toJsonSub(@NonNull JSONObject object) {
        toJsonQuestionOnlySub(object);
    }

    @Override
    public void toJsonQuestionOnlySub(@NonNull JSONObject object) {
        JSONArray itemsArray = new JSONArray();
        for (Map.Entry<Integer, ChoiceNode> choice : items.entrySet())
        {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", choice.getKey());
            if (choice.getValue().img != null)
                jsonObject.put("image", Utils.encodeImageToBase64(choice.getValue().img));
            if (choice.getValue().text != null && !choice.getValue().text.isEmpty())
                jsonObject.put("text", choice.getValue().text);
            itemsArray.put(jsonObject);
        }
        object.put("items", itemsArray);

        JSONArray targetsArray = new JSONArray();
        for (Map.Entry<Integer, ChoiceNode> choice : targets.entrySet())
        {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", choice.getKey());
            if (choice.getValue().img != null)
                jsonObject.put("image", Utils.encodeImageToBase64(choice.getValue().img));
            if (choice.getValue().text != null && !choice.getValue().text.isEmpty())
                jsonObject.put("text", choice.getValue().text);
            targetsArray.put(jsonObject);
        }
        object.put("targets", targetsArray);
    }

    @Override
    public Answer getDefaultAnswer() {
        return new Answer();
    }

    @Override
    public Answer compareAnswer(@Nullable JSONObject jsonObject) {

        Answer answer = new Answer();
        answer.setValidity(Answer.Validity.WRONG);
        answer.setPoints(0);
        answer.setRightAnswer("");

        if (jsonObject == null) {
            return answer;
        }

        double timePassed = -1.0;
        double timeFactor = 1.0;
        if (jsonObject.has("timePassed")) {
            timePassed = jsonObject.getDouble("timePassed");
        }

        if (timePassed != -1.0) {
            timeFactor = 1.0 - (timePassed / (double) this.timeLimit / 2.0);
        }



        int correctMade = 0;
        int correctTotal = targets.size();
        JSONArray jsonArray = jsonObject.getJSONArray("pairs");
        for (Object pair : jsonArray)
        {
            JSONObject jsonPair = (JSONObject) pair;
            int targetId = jsonPair.getInt("targetId");
            if (jsonPair.has("itemId"))
            {
                int itemId = jsonPair.getInt("itemId");
                if (!items.containsKey(itemId) || !targets.containsKey(targetId))
                    continue;

                ChoiceNode nodeItem = items.get(itemId);
                ChoiceNode nodeTarget = targets.get(targetId);
                if (nodeItem.id == nodeTarget.id)
                    correctMade += 1;
            }
        }

        answer.setValidity(correctTotal, correctMade);
        answer.setPoints((int)(points * timeFactor * ((double) correctMade / (double)correctTotal) ));

        return answer;
    }

    private static int generateUniqueRandomKey(Map<Integer, ChoiceNode> map, Random random) {
        int randomKey;
        do {
            // Generate a random integer
            randomKey = random.nextInt();
        } while (map.containsKey(randomKey)); // Ensure the key is unique
        return randomKey;
    }

}
