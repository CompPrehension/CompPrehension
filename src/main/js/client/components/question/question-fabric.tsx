import * as React from 'react';
import store from '../../store';
import { observer } from 'mobx-react';
import { OrderQuestion } from "./question-types/order-question";
import { MatchingQuestion } from './question-types/matching-question';
import { Loader } from '../loader';

export const QuestionFabric = observer(() => {  
    if (store.isQuestionLoading) {
        return <Loader />;
    }

    const questionData = store.questionData;
    if (!questionData) {
        return null;
    }

    switch (true) {        
        case questionData.type == "MATCHING":
            return <MatchingQuestion />;
        case questionData.type == "ORDER" && questionData.options.requireContext:
            return <OrderQuestion />;
    }

    return (<div>Unsupported question type</div>);
});
