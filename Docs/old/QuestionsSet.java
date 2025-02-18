package com.igalblech.igalsquizserver.Questions;

import com.igalblech.igalsquizserver.UserData;
import lombok.Getter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Deprecated
public class QuestionsSet
{
    public List<QuestionBase> list = new ArrayList<>();

    Iterator<QuestionBase> iterator;


    public static QuestionBase getQuestionObjectFromName(String name) throws ClassNotFoundException
    {
        String clsStr = "com.igalblech.igalsquizserver.Questions." + name;
        try {
            Class cls = Class.forName(clsStr);
            return (QuestionBase) cls.getDeclaredConstructor().newInstance();
        }
        catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("QuestionsSet {\n  questions: [");

        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i).toString()); // Assuming QuestionBase has a toString method
            if (i < list.size() - 1) {
                sb.append(",\n");
            }
        }
        sb.append("\n  ]\n}");
        return sb.toString();
    }

    public void loadFromJson(JSONObject jsonQuestions) throws ClassNotFoundException {
        this.list.clear();
        assert jsonQuestions != null;

        JSONArray arrayQuestions;
        arrayQuestions = jsonQuestions.getJSONArray("questions");

        for (int i = 0; i < arrayQuestions.length(); i++)
        {
            JSONObject jsonQuestion;
            jsonQuestion = (JSONObject) arrayQuestions.get(i);

            String questionType = jsonQuestion.getString("question_type");

            QuestionBase base;
            base = getQuestionObjectFromName(questionType);
            base.loadJson(jsonQuestion);

            this.list.add(base);
        }

        this.iterator = this.list.iterator();
    }
}
