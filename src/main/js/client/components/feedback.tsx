import { CircularProgress } from '@material-ui/core';
import { observer } from 'mobx-react';
import * as React from 'react';
import store from '../store';


export const Feedback = observer(() => {
    const { feedbackMessages, isFeedbackLoading } = store;
    
    if (store.isFeedbackLoading) {
        return <CircularProgress />;
    }

    if (!feedbackMessages) {
        return null;
    }

    return (
        <div className="comp-ph-feedback-wrapper">
            {feedbackMessages.map(m => <div className="comp-ph-feedback-error">{m}</div>)}
        </div>
    );
});
