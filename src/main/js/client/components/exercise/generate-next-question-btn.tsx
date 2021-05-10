
import { observer } from 'mobx-react';
import * as React from 'react';
import { useState } from 'react';
import { Button } from 'react-bootstrap';
import { container } from "tsyringe";
import { ExerciseStore } from "../../stores/exercise-store";

export const GenerateNextQuestionBtn = observer(() => {
    const [exerciseStore] = useState(() => container.resolve(ExerciseStore));
    const onClicked = () => {
        exerciseStore.currentQuestion.generateQuestion(exerciseStore.currentAttempt?.attemptId ?? -1);
    };

    const { sessionInfo, currentAttempt } = exerciseStore;
    const { question } = exerciseStore.currentQuestion;
    if (!question || !sessionInfo || !currentAttempt) {
        return null;
    }

    const { user } = sessionInfo;
    const { questionIds } = currentAttempt;

    // show btn if user is admin or if he is on the last question and has finished it
    if (!user.roles.includes('ADMIN') && !user.roles.includes('TEACHER') &&
        (questionIds[questionIds.length - 1] !== question.questionId || exerciseStore.currentQuestion.feedback?.stepsLeft !== 0)) {
        return null;
    }

    return (
        <div style={{ marginTop: '20px'}}>            
            <Button onClick={onClicked} variant="primary" >Next question</Button>
        </div>
    )
})

