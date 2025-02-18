package com.igalblech.igalsquizserver.Questions;

import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

public class QuestionMinigame extends QuestionBase implements Cloneable {
    @Getter
    private int gameDifficulty = 0;

    @Getter
    private String gameName = "";

    public QuestionMinigame() {
        super();
    }

    @Override
    public void loadJsonSub(@NonNull JSONObject object) {

        System.out.println("Data in: " + object.get("game_difficulty") + ", " + object.get("game_name"));

        if (object.has("game_difficulty"))
        {
            this.gameDifficulty = object.getInt("game_difficulty");
        }
        if (object.has("game_name"))
        {
            this.gameName = object.getString("game_name");
        }
    }

    @Override
    public void toJsonSub(@NonNull JSONObject object) {
        toJsonQuestionOnlySub(object);
    }

    @Override
    public void toJsonQuestionOnlySub(@NonNull JSONObject out) {
        System.out.println("Data out:" + gameDifficulty + ", " + gameName);
        out.put("gameDifficulty", gameDifficulty);
        out.put("gameName", gameName);
    }

    @Override
    public Answer compareAnswer(@Nullable JSONObject jsonObject) {
        double timeFactor = 1.0;
        double timePassed = -1.0;
        double success = 1.0;

        if (jsonObject == null)
        {
            success = 0;
        }
        else {
            if (jsonObject.has("timePassed")) {
                timePassed = jsonObject.getDouble("timePassed");
            }

            if (jsonObject.has("success"))
                success = jsonObject.getDouble("success");

            if (timePassed != -1.0)
                timeFactor = 1.0 - (timePassed / (double) this.timeLimit / 2.0);
        }

        Answer answer = new Answer();
        answer.setPoints((int)(points * timeFactor * success));
        answer.validable = false;

        switch ((int) success)
        {
            case 1:
                answer.validity = Answer.Validity.RIGHT;
                break;
            case 0:
                answer.validity = Answer.Validity.WRONG;
                break;
            default:
                answer.validity = Answer.Validity.PARTIALLY_RIGHT;
                break;
        }

        return answer;
    }

    @Override
    public QuestionMinigame clone() {
        try {
            QuestionMinigame clone = (QuestionMinigame) super.clone();
            clone.gameDifficulty = this.gameDifficulty;
            clone.gameName = this.gameName;
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
