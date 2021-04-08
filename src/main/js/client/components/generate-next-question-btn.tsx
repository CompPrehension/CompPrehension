
import { observer } from 'mobx-react';
import * as React from 'react';
import { Button } from 'react-bootstrap';
import store from '../store';


export const GenerateNextQuestionBtn = observer(() => {
    const onClicked = () => {
        store.generateQuestion();
    };

    const { sessionInfo, questionData } = store;
    if (!sessionInfo || !questionData) {
        return null;
    }

    const { user, questionIds } = sessionInfo;

    // show btn if user is admin or if he is on the last question and has finished it
    if (!user.roles.includes('ADMIN') && !user.roles.includes('TEACHER') &&
        (questionIds[questionIds.length - 1] !== questionData.questionId || store.feedback?.stepsLeft !== 0)) {
        return null;
    }

    return (
        <div style={{ marginTop: '20px'}}>            
            <Button onClick={onClicked} variant="primary" >Next question</Button>
        </div>
    )
})

