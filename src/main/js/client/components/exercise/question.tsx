import { observer } from "mobx-react";
import React from "react";
import { QuestionStore } from "../../stores/question-store";
import { Loader } from "../common/loader";
import { QuestionComponent } from "../common/question/question";

export const Question = observer(({ store }: { store: QuestionStore }) => {
    const questionData = store.question;
    if (store.isQuestionLoading) {
        return <Loader />;
    }
    if (!questionData) {
        return null;
    }    

    const onChanged = (newHistory: [number, number][]) => {
        if (store.isHistoryChanged(newHistory)) {
            store.updateAnswersHistory(newHistory);
        }
    }

    return <QuestionComponent question={questionData} answers={store.answersHistory} onChanged={onChanged} />;
})
