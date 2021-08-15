
import { observer } from 'mobx-react';
import * as React from 'react';
import { useState } from 'react';
import { Button } from 'react-bootstrap';
import { container } from "tsyringe";
import { ExerciseStore } from "../../stores/exercise-store";

export const GenerateNextAnswerBtn = observer(() => {
    const [exerciseStore] = useState(() => container.resolve(ExerciseStore));
    if (!exerciseStore.sessionInfo?.exercise.options.correctAnswerGenerationEnabled) {
        return null;
    }
    
    const onClicked = () => {
        exerciseStore.currentQuestion.generateNextCorrectAnswer();
    };
    const { sessionInfo, currentAttempt } = exerciseStore;
    const { question } = exerciseStore.currentQuestion;
    if (!question || !sessionInfo || !currentAttempt) {
        return null;
    }

    return (
        <div style={{ marginTop: '20px'}}>            
            <Button onClick={onClicked} variant="primary">Next correct answer</Button>
        </div>
    )
})
