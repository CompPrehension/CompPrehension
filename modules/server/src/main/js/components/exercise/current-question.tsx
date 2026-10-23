import { observer } from 'mobx-react';
import * as React from 'react';
import { getExerciseStore } from "../../stores/exercise-store";
import { Question } from './question';

export const CurrentQuestion = observer(() => {
    const exerciseStore = getExerciseStore();
    return <Question store={exerciseStore.currentQuestion} showExtendedFeedback={exerciseStore.exercise?.options.supplementaryQuestionsEnabled ?? true}/>;
});
