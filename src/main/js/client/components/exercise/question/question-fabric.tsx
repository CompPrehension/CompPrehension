import * as React from 'react';
import { exerciseStore } from '../../../stores/exercise-store';
import { observer } from 'mobx-react';
import { OrderQuestion } from "./question-types/order-question";
import { MatchingQuestion } from './question-types/matching-question';
import { Loader } from '../../common/loader';

export const QuestionFabric = observer(() => {  
    if (exerciseStore.isQuestionLoading) {
        return <Loader />;
    }

    const questionData = exerciseStore.currentQuestion;
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
