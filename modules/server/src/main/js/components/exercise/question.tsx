import { observer } from "mobx-react";
import React from "react";
import { QuestionStore } from "../../stores/question-store";
import { Loader } from "../common/loader";
import { QuestionComponent } from "../common/question/question";
import { Answer } from "../../types/answer";
import { Feedback } from "./feedback";

type QuestionOptions = {
    store: QuestionStore, 
    showExtendedFeedback: boolean,
    onChanged?: (newHistory: Answer[]) => void,
}
export const Question = observer((props: QuestionOptions) => {
    const { store, showExtendedFeedback, onChanged:ParentOnChanged } = props;
    const questionData = store.question;
    if (store.questionState === 'LOADING') {
        return <div className="mt-2"><Loader /></div>;
    }
    if (!questionData) {
        return null;
    }    

    const onChanged = async (newHistory: Answer[]) => {
        if (await store.setFullAnswer(newHistory)) {
            ParentOnChanged?.(newHistory);
        }
    };
    const getAnswer = () => store.lastAnswer as Answer[];
    const getFeedback = () => store.feedback;

    return (
        <>
            <QuestionComponent question={questionData} answers={store.lastAnswer as Answer[]} getAnswers={getAnswer} onChanged={onChanged} getFeedback={getFeedback} isFeedbackLoading={store.questionState === 'ANSWER_EVALUATING'} isQuestionFreezed={store.isQuestionFreezed}/>
            <Feedback store={store} showExtendedFeedback={showExtendedFeedback}/>
        </>
    );
})


