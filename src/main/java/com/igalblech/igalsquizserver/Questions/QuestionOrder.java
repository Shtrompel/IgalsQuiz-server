package com.igalblech.igalsquizserver.Questions;

import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class QuestionOrder extends QuestionBase implements Cloneable {
    @Getter
    private Map<String, Choice> answers = new TreeMap<>();
    @Getter
    private List<Choice> answersOut  = null;
    private Map<Integer, Choice> answersIndx = new TreeMap<>();

    public QuestionOrder() {
        super();
    }

    @Override
    public Object clone() {
        final QuestionOrder clone;
        try {
            clone = (QuestionOrder) super.clone();
        }
        catch (CloneNotSupportedException ex) {
            throw new RuntimeException("superclass messed up", ex);
        }
        clone.answers = new TreeMap<>(this.answers);
        clone.answersIndx = new TreeMap<>(this.answersIndx);
        return clone;
    }

    @Override
    public void loadJsonSub(JSONObject object) throws JSONException {

        System.out.println(object.toString());

        if (!object.has("order_answers"))
        {
            return;
        }

        JSONArray jsonAnswers = object.getJSONArray("order_answers");
        this.answersOut = new ArrayList<>();
        for (Object choice : jsonAnswers) {
            JSONObject jsonChoice = (JSONObject) choice;
            Choice choiceObj = new Choice();
            choiceObj.loadJson(jsonChoice);
            if (choiceObj.getOrder() != -1) {
                this.answers.put(choiceObj.getText(), choiceObj);
                this.answersIndx.put(choiceObj.getOrder(), choiceObj);
                this.answersOut.add(choiceObj);
            }
        }

        if (this.randomize) {
            java.util.Collections.shuffle(this.answersOut);
        }
    }

    @Override
    public void toJsonSub(@NonNull JSONObject object) {
        toJsonQuestionOnlySub(object);
    }

    @Override
    public void toJsonQuestionOnlySub(@NonNull JSONObject object) {
        JSONArray array = new JSONArray();
        for (Choice choice : this.answersOut)
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
            Answer answer = new Answer();
            answer.validity = Answer.Validity.WRONG;
            answer.points = 0;
            answer.rightAnswer = "";
            answer.validable = true;
            return answer;
        }

        double timePassed = -1.0, timeFactor = 1.0;
        if (jsonObject.has("timePassed"))
            timePassed = jsonObject.getDouble("timePassed");
        if (timePassed != -1.0)
            timeFactor = 1.0 - (timePassed / (double)this.timeLimit / 2.0);

        /*
        If Correct Order is:    A B C D E
        And answer is:          A D E B C
        nums will be:           0 3 4 1 2

        And indexes will be:    0 3 4 1 2
         (the index of 0 is 0, the index of 2 is 4, the index of 3 is 5

         */

        String trueOrder = "";
        for (Choice choice : answersIndx.values())
        {
            trueOrder += choice.getText();
            trueOrder += " -> ";
        }
        if (!trueOrder.isEmpty())
        {
            trueOrder = trueOrder.substring(0, trueOrder.length() - 4);
        }


        JSONArray answers = jsonObject.getJSONArray("answers");
        int[] nums = new int[answers.length()];
        for (int i = 0; i < answers.length(); i++)
        {
            String answer = (String)answers.get(i);
            nums[i] = this.answers.get(answer).getOrder() - 1;
        }

        int rightCounter = 0;
        int lastIndex = -1;
        for (int i = 0; i < nums.length; i++)
        {
            int index = -1;
            for (int j = 0; j < nums.length; j++)
            {
                if (nums[j] == i)
                {
                    index = j;
                    break;
                }
            }

            System.out.println("" + i + ", " + index);

            if (index != -1)
            {
                if (lastIndex != -1 && index > lastIndex)
                {
                    rightCounter++;
                }
                lastIndex = index;
            }
        }
        if (rightCounter > 0)
            rightCounter++;

        // rightCounter timePassed

        System.out.println(points + ", " + timeFactor + ", " + ((double) rightCounter / (double)nums.length));

        Answer answer = new Answer();
        answer.setValidity(nums.length, rightCounter);
        answer.setPoints((int)(points * timeFactor * ((double) rightCounter / (double)nums.length)));
        answer.setRightAnswer(trueOrder);

        return answer;
    }

}
