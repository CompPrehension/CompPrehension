
import { observer } from 'mobx-react';
import * as React from 'react';
import { Button } from 'react-bootstrap';
import { exerciseStore } from "../stores/exercise-store";


export const GenerateNextQuestionBtn = observer(() => {
    const onClicked = () => {
        exerciseStore.generateQuestion();
    };

    const { sessionInfo, currentQuestion, currentAttempt } = exerciseStore;
    if (!currentQuestion || !sessionInfo || !currentAttempt) {
        return null;
    }

    const { user } = sessionInfo;
    const { questionIds } = currentAttempt;

    // show btn if user is admin or if he is on the last question and has finished it
    if (!user.roles.includes('ADMIN') && !user.roles.includes('TEACHER') &&
        (questionIds[questionIds.length - 1] !== currentQuestion.questionId || exerciseStore.feedback?.stepsLeft !== 0)) {
        return null;
    }

    return (
        <div style={{ marginTop: '20px'}}>            
            <Button onClick={onClicked} variant="primary" >Next question</Button>
        </div>
    )
})

