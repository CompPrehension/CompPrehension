
import { Button } from '@material-ui/core';
import { observer } from 'mobx-react';
import * as React from 'react';
import store from '../store';


export const NextQuestionBtn = observer(() => {
    const onClicked = () => {
        const qId = store.questionData?.id ?? '';
        if (qId) {
            const { attemptIds=[] } = store.sessionInfo ?? {};
            const idx = attemptIds.indexOf(qId) ?? -1;
            if (idx > -1) {
                store.loadQuestion(attemptIds[(idx + 1) % attemptIds.length]);
            }
        }
    }

    return (
        <div style={{ marginTop: '20px'}}>            
            <Button onClick={onClicked} variant="contained" color="primary">Следующий вопрос</Button>
        </div>
    )
})

