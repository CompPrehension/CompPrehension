import * as React from 'react';
import { observer } from 'mobx-react';
import { OrderQuestion } from "./question-types/order-question";
import { MatchingQuestion } from './question-types/matching-question';
import { Loader } from '../../common/loader';
import { SingleChoiceQuestion } from './question-types/choice-question';
import { container } from "tsyringe";
import { ExerciseStore } from "../../../stores/exercise-store";

const exerciseStore = container.resolve<ExerciseStore>(ExerciseStore);

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
        case questionData.type == "SINGLE_CHOICE":
            return <SingleChoiceQuestion />;
    }

    return (<div>Unsupported question type</div>);
});
