import * as React from 'react';
import { QuestionType } from '../../typings/question.d';
import store from '../../store';
import { observer } from 'mobx-react';
import { OrderQuestion } from "./question-types/order-question";
import { SingleChoiceQuestion } from './question-types/single-choice-question';
import { MultiChoiceQuestion } from './question-types/multi-choice-question';

export const QuestionFabric = observer(() => {
    const questionData = store.questionData;
    if (!questionData) {
        return null;
    }

    switch (questionData.type) {
        //case QuestionType.SINGLE_CHOICE:
        //    return <SingleChoiceQuestion question={questionData} onSelectionChanged={(id) => store.onAnswersChanged(id)} selectedCheckboxId={store.getAnswers()} />;
        //case QuestionType.MULTI_CHOICE:
        //    return <MultiChoiceQuestion question={questionData} onSelectionChanged={answ => store.onAnswersChanged(answ)} selectedCheckboxes={store.getAnswers()}/>;
        case QuestionType.ORDER:
            return <OrderQuestion />;
    }

    return (<div>Unsupported question type</div>);
});
