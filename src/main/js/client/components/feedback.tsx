import { observer } from 'mobx-react';
import * as React from 'react';
import { Alert, Spinner } from 'react-bootstrap';
import store from '../store';
import { Loader } from './loader';


export const Feedback = observer(() => {
    const { feedbackMessages, isFeedbackLoading } = store;
    
    if (store.isFeedbackLoading) {
        return <Loader />;
    }

    if (feedbackMessages === undefined) {
        return null;
    }

    if (feedbackMessages.length === 0) {
        return (
            <div className="comp-ph-feedback-wrapper">
                <Alert variant="success">
                    {"Correct, keep doing..."}
                </Alert>       
            </div>     
        );
    }

    return (
        <div className="comp-ph-feedback-wrapper">
            {feedbackMessages.map(m => <Alert variant="danger">{m}</Alert>)}
        </div>
    );
});
