
import { observer } from 'mobx-react';
import * as React from 'react';
import { Button } from 'react-bootstrap';
import store from '../store';


export const GenerateNextQuestionBtn = observer(() => {
    const onClicked = () => {
        store.generateQuestion();
    };

    if (!store.sessionInfo) {
        return null;
    }

    return (
        <div style={{ marginTop: '20px'}}>            
            <Button onClick={onClicked} variant="primary" >Generate next question</Button>
        </div>
    )
})

