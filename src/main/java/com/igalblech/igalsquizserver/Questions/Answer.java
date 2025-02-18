package com.igalblech.igalsquizserver.Questions;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.json.JSONObject;

@Getter
@Setter
@ToString
public class Answer {

    public enum Validity {
        RIGHT("Right"),
        PARTIALLY_RIGHT("Partially Right"),
        WRONG("Wrong");

        public String str;
        Validity(String str)
        {
            this.str = str;
        }
    }

    Validity validity = Validity.WRONG;
    int points = 0;
    String rightAnswer = "";
    boolean validable = true;

    // While when the player answers a question the data of the answer do not get saved,
    // in the painting minigame the picture has to be saved for the host to evaluate them.
    JSONObject externalData = null;

    public JSONObject toJson() {
        JSONObject out = new JSONObject();
        out.put("validness", validity.str);
        out.put("points", points);
        out.put("rightAnswer", rightAnswer);
        out.put("validable", validable);
        if (externalData != null)
            out.put("externalData", externalData);
        return out;
    }

    void setValidity(int correctCount, int correctAnswered)
    {
        if (correctAnswered == 0)
            validity = Validity.WRONG;
        else if (correctCount == correctAnswered)
            validity = Validity.RIGHT;
        else
            validity = Validity.PARTIALLY_RIGHT;
    }


}
