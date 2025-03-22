package com.igalblech.igalsquizserver.Questions;

import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Console;
import java.util.*;

public class QuestionChoice extends QuestionBase implements Cloneable {

    @Getter
    private List<Choice> choices = new ArrayList<>();
    private List<Choice> choicesOut = new ArrayList<>();
    private List<Choice> correctChoices = new ArrayList<>();
    private Map<Integer, List<Choice>> groups = new HashMap<>();

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
            if (choiceObj.isRight())
            {
                if (!groups.containsKey(choiceObj.getGroup())) {
                    List<Choice> list = new ArrayList<>();
                    list.add(choiceObj);
                    groups.put(choiceObj.getGroup(), list);
                }
                else
                    groups.get(choiceObj.getGroup()).add(choiceObj);
            }
         }

        for (Choice choice : choices)
        {
            if (choice.isRight())
                correctChoices.add(choice);
        }

        this.choicesOut = new ArrayList<>(this.choices);
        if (this.randomize) {
            java.util.Collections.shuffle(this.choicesOut);
        }
    }

    @Override
    public void toJsonSub(@NonNull JSONObject object) {
        toJsonQuestionOnlySub(object);
    }

    @Override
    public void toJsonQuestionOnlySub(@NonNull JSONObject object) {
        JSONArray array = new JSONArray();

        for (Choice choice : choicesOut)
        {
            array.put(choice.toJson());
        }

        int max = -1;
        if (this.groups.isEmpty())
        {
            max = correctChoices.size();
        }
        else {
            for (List<Choice> choice : this.groups.values()) {
                max = Math.max(max, choice.size());
            }
        }

        object.put("choices", array);
        object.put("maxChoices", max);
    }

    @Override
    public Answer getDefaultAnswer() {
        return new Answer();
    }

    @Override
    public Answer compareAnswer(@Nullable JSONObject jsonObject) {

        if (jsonObject == null) {
            return getAnswer(-1, 0, 0);
        }

        double timePassed = -1.0;
        if (jsonObject.has("timePassed")) {
            timePassed = jsonObject.getDouble("timePassed");
        }

        int correctMade = 0;
        int correctTotal = 0;

        if (jsonObject.has("answers")) {
            JSONArray jsonAnswers = jsonObject.getJSONArray("answers");
            if (groups.isEmpty()) {
                correctTotal = correctChoices.size();

                for (Choice choice : choices) {
                    // If choice is not a right answer, no point in checking it
                    if (!choice.isRight())
                        continue;

                    // Find if any of the answers match
                    for (int i = 0; i < jsonAnswers.length(); i++) {
                        String answer = jsonAnswers.getString(i);
                        if (!answer.equals(choice.getText()))
                            break;
                        correctMade += 1;
                    }
                }
            } else {
                // There are multiple groups that can be right, and the player can select answers from any group
                // todo finish comment
                int maxCorrect = -1;
                int maxTotal = -1;
                System.out.println(jsonAnswers);
                for (List<Choice> choices : this.groups.values()) {
                    int correctCurrent = 0;
                    for (Choice choice : choices) {
                        for (int i = 0; i < jsonAnswers.length(); i++) {
                            String answer = jsonAnswers.getString(i);
                            if (!answer.equals(choice.getText()))
                                continue;
                            correctCurrent += 1;
                        }
                    }

                    if (maxCorrect == -1 || (double) correctCurrent / choices.size() > (double) maxCorrect / maxTotal) {
                        maxCorrect = correctCurrent;
                        maxTotal = choices.size();
                    }

                }
                correctTotal = maxTotal;
                correctMade = maxCorrect;
            }
        }

        return getAnswer(timePassed, correctMade, correctTotal);
    }

    private @NotNull Answer getAnswer(double timePassed, int correctMade, int correctTotal) {
        double timeFactor = 1.0;

        if (timePassed != -1.0)
            timeFactor = 1.0 - (timePassed / (double)this.timeLimit / 2.0);

        assert correctChoices.size() == 0;

        String rightAnswer;
        if (correctChoices.size() == 1)
        {
            rightAnswer = "The right answer was " + correctChoices.getFirst().getText();
        }
        else if (groups.isEmpty())
        {
            rightAnswer = "The answers were ";
            for (int i = 0; i < correctChoices.size(); i++)
            {
                rightAnswer += correctChoices.get(i).getText();
                if (i != correctChoices.size() - 1)
                    rightAnswer += " and ";
            }
        }
        else
        {
            List<Choice>[] choicesGroups = groups.values().stream()
                    .toArray(List[]::new);
            rightAnswer = "The answers were ";
            for (int i = 0; i < choicesGroups.length; i++)
            {
                var choices = choicesGroups[i];
                String groupStr = "";
                for (int j = 0; j < choices.size(); j++)
                {
                    groupStr += choices.get(j).getText();
                    if (j != choices.size() - 1)
                        groupStr += " and ";
                }
                rightAnswer += groupStr;
                if (i != choicesGroups.length - 1)
                    rightAnswer += " or ";
            }
        }

        Answer answer = new Answer();
        answer.setValidity(correctTotal, correctMade);
        if (correctTotal != 0)
            answer.setPoints((int)(points * timeFactor * ((double) correctMade / (double)correctTotal)));
        else
            answer.setPoints(0);
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
