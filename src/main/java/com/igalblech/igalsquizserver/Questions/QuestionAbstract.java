package com.igalblech.igalsquizserver.Questions;

import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

public interface QuestionAbstract {

    void loadJsonSub(@NonNull JSONObject object);

    void toJsonSub(@NonNull JSONObject object);

    void toJsonQuestionOnlySub(@NonNull JSONObject out);

    Answer getDefaultAnswer();

    Answer compareAnswer(@Nullable JSONObject jsonObject);
}
