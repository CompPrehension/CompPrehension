import { observer } from "mobx-react";
import React from "react";
import { Feedback } from "../../../types/feedback";
import { Question } from "../../../types/question"
import { MatchingQuestionComponent } from "./matching-question";
import { MultiChoiceQuestionComponent } from "./multi-choice-question";
import { OrderQuestionComponent } from "./order-question";
import { SingleChoiceQuestionComponent } from "./single-choice-question";

type QuestionComponentProps = {
    question: Question,
    feedback?: Feedback, 
    answers: [number, number][],
    getAnswers: () => [number, number][],
    onChanged: (x: [number, number][]) => void,
}

export const QuestionComponent = observer((props: QuestionComponentProps) => {
    const { question, answers, onChanged, getAnswers, feedback } = props;
    switch(question.type) {
        case 'MATCHING':                
            return <MatchingQuestionComponent question={question} onChanged={onChanged} answers={answers} getAnswers={getAnswers}/>;
        case 'MULTI_CHOICE':
            return <MultiChoiceQuestionComponent question={question} onChanged={onChanged} answers={answers} getAnswers={getAnswers}/>;
        case 'SINGLE_CHOICE':
            return <SingleChoiceQuestionComponent question={question} onChanged={onChanged} answers={answers} getAnswers={getAnswers}/>;
        case 'ORDER':
            return <OrderQuestionComponent question={question} onChanged={onChanged} answers={answers} getAnswers={getAnswers} feedback={feedback}/>;
    }
    return (<div>Not implemented</div>);
})
