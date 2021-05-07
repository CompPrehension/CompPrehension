import { observer } from 'mobx-react';
import * as React from 'react';
import { useState } from 'react';
import { container } from "tsyringe";
import { ExerciseStore } from '../../stores/exercise-store';
import { Loader } from '../common/loader';
import { QuestionComponent } from '../common/question/question';


export const CurrentQuestion = observer(() => {
    const [exerciseStore] = useState(() => container.resolve(ExerciseStore));
    const questionData = exerciseStore.currentQuestion;
    if (exerciseStore.isQuestionLoading) {
        return <Loader />;
    }
    if (!questionData) {
        return null;
    }    

    const onChanged = (newHistory: [number, number][]) => {
        if (exerciseStore.isHistoryChanged(newHistory)) {
            exerciseStore.updateAnswersHistory(newHistory);
        }
    }

    return <QuestionComponent question={questionData} answers={exerciseStore.answersHistory} onChanged={onChanged} />;
});
