package com.igalblech.igalsquizserver.Questions;

import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class QuestionChoice extends QuestionBase implements Cloneable {

    @Getter
    private List<Choice> choices = new ArrayList<>();
    private List<Choice> correctChoices = new ArrayList<>();

    public QuestionChoice() {
        super();
    }

    @Override
    public Object clone() {
        final QuestionChoice clone;
        try {
            clone = (QuestionChoice) super.clone();
        }
        catch (CloneNotSupportedException ex) {
            throw new RuntimeException("superclass messed up", ex);
        }
        clone.choices = new ArrayList<>(this.choices);
        clone.correctChoices = new ArrayList<>(this.correctChoices);
        return clone;
    }

    @Override
    public void loadJsonSub(JSONObject object) throws JSONException {

        if (!object.has("choice_answers"))
        {
            return;
        }

        for (Object choice : object.getJSONArray("choice_answers")) {
            JSONObject jsonChoice = (JSONObject) choice;
            Choice choiceObj = new Choice();
            choiceObj.loadJson(jsonChoice);
            choices.add(choiceObj);
         }

        for (Choice choice : choices)
        {
            if (choice.isRight())
                correctChoices.add(choice);
        }
    }

    @Override
    public void toJsonSub(@NonNull JSONObject object) {
        toJsonQuestionOnlySub(object);
    }

    @Override
    public void toJsonQuestionOnlySub(@NonNull JSONObject object) {
        JSONArray array = new JSONArray();
        for (Choice choice : choices)
        {
            array.put(choice.toJson());
        }
        object.put("choices", array);
    }

    @Override
    public Answer getDefaultAnswer() {
        return new Answer();
    }

    @Override
    public Answer compareAnswer(@Nullable JSONObject jsonObject) {

        if (jsonObject == null)
        {
            return getAnswer(-1, 0);
        }

        double timePassed = -1.0;
        if (jsonObject.has("timePassed")) {
            timePassed = jsonObject.getDouble("timePassed");
        }

        int correctMade = 0;


        for (Choice choice : choices) {
            // If choice is not a right answer, no point in checking it
            if (!choice.isRight())
                continue;

            // Find if any of the answers match
            if (jsonObject.has("answers")) {

                JSONArray jsonAnswers = jsonObject.getJSONArray("answers");

                for (int i = 0; i < jsonAnswers.length(); i++) {
                    String answer = jsonAnswers.getString(i);
                    if (answer.equals(choice.getText())) {
                        correctMade += 1;
                        break;
                    }
                }
            }
        }

        Answer answer = getAnswer(timePassed, correctMade);
        return getAnswer(timePassed, correctMade);
    }

    private @NotNull Answer getAnswer(double timePassed, int correctMade) {
        double timeFactor = 1.0;

        if (timePassed != -1.0)
            timeFactor = 1.0 - (timePassed / (double)this.timeLimit / 2.0);

        assert correctChoices.size() == 0;

        String rightAnswer;
        if (correctChoices.size() == 1)
        {
            rightAnswer = "The right answer was " + correctChoices.getFirst().getText();
        }
        else
        {
            rightAnswer = "The answers were ";
            for (int i = 0; i < correctChoices.size(); i++)
            {
                rightAnswer += correctChoices.get(i).getText();
                if (i != correctChoices.size() - 1)
                    rightAnswer += " and ";
            }
        }

        Answer answer = new Answer();
        answer.setValidity(correctChoices.size(), correctMade);
        answer.setPoints((int)(points * timeFactor * ((double) correctMade / (double)correctChoices.size())));
        answer.setRightAnswer(rightAnswer);

        return answer;
    }


    @Override
    public String toString() {
        StringBuilder choicesStr = new StringBuilder();
        for (Choice choice : choices) {
            choicesStr.append(choice.toString()).append(", ");
        }

        // Remove the trailing comma and space, if there are any choices
        if (!choicesStr.isEmpty()) {
            choicesStr.setLength(choicesStr.length() - 2);
        }

        return String.format("%s,\n  choices: [%s\n  ]\n}",
                super.toString(), choicesStr);
    }
}
