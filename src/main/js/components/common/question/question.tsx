import { absurd } from "fp-ts/lib/function";
import { observer } from "mobx-react";
import React from "react";
import { Answer } from "../../../types/answer";
import { Feedback } from "../../../types/feedback";
import { Question } from "../../../types/question"
import { MatchingQuestionComponent } from "./matching-question";
import { MultiChoiceQuestionComponent } from "./multi-choice-question";
import { OrderQuestionComponent } from "./order-question";
import { SingleChoiceQuestionComponent } from "./single-choice-question";

type QuestionComponentProps = {
    question: Question,
    getFeedback: () => Feedback | undefined,
    isFeedbackLoading: boolean,
    isQuestionFreezed: boolean,
    answers: Answer[],
    getAnswers: () => Answer[],
    onChanged: (x: Answer[]) => void,
}

export const QuestionComponent = observer((props: QuestionComponentProps) => {
    const { question, answers, onChanged, getAnswers, getFeedback, isFeedbackLoading, isQuestionFreezed } = props;
    let questonComponent: JSX.Element;
    switch(question.type) {
        case 'MATCHING':                
            questonComponent = <MatchingQuestionComponent question={question} onChanged={onChanged} answers={answers} getAnswers={getAnswers}/>;
            break;
        case 'MULTI_CHOICE':
            questonComponent = <MultiChoiceQuestionComponent question={question} onChanged={onChanged} answers={answers} getAnswers={getAnswers}/>;
            break;
        case 'SINGLE_CHOICE':
            questonComponent = <SingleChoiceQuestionComponent question={question} onChanged={onChanged} answers={answers} getAnswers={getAnswers}/>;
            break;
        case 'ORDER':
            questonComponent = <OrderQuestionComponent question={question} onChanged={onChanged} answers={answers} getAnswers={getAnswers} getFeedback={getFeedback}/>;
            break;
        default:
            // compile-time checking whether the question has `never` type 
            // to ensure that all case branches have been processed
            return absurd<JSX.Element>(question);
    }

    const wrapperClassName = [
        "comp-ph-question-wrapper",
        getFeedback()?.stepsLeft === 0 && "comp-ph-question-wrapper--finished" || "",
        isFeedbackLoading && "comp-ph-question-wrapper--loading-feedback" || "",
        isQuestionFreezed && "comp-ph-question-wrapper--freezed" || "",
    ].join(' ');

    return (
        <div className={wrapperClassName}>
            {questonComponent}
        </div>
    );
})
