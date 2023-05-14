import { observer } from 'mobx-react';
import * as React from 'react';
import { useState } from 'react';
import { container } from "tsyringe";
import { ExerciseStore } from '../../stores/exercise-store';
import { Question } from './question';

export const CurrentQuestion = observer(() => {
    const [exerciseStore] = useState(() => container.resolve(ExerciseStore));
    return <Question store={exerciseStore.currentQuestion} showExtendedFeedback={exerciseStore.exercise?.options.supplementaryQuestionsEnabled ?? true}/>;
});
