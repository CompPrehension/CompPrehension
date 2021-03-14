import { observer } from 'mobx-react';
import * as React from 'react';
import { Alert, Spinner } from 'react-bootstrap';
import store from '../store';
import { Feedback as FeedbackType } from '../types/feedback';
import { Loader } from './loader';


export const Feedback = observer(() => {
    const { feedback, isFeedbackLoading } = store;
    
    if (store.isFeedbackLoading) {
        return <Loader />;
    }

    if (!feedback) {
        return null;
    }

    return (
        <div className="comp-ph-feedback-wrapper">            
            <p>
                {renderFeedback(feedback)}                
            </p>
            {/*<p>Current grade: {feedback.grade}</p>*/}           
        </div>
    );
});

const renderFeedback = (feedback: FeedbackType) => {
    const state = (!feedback.errors.length && feedback.iterationsLeft === 0) ? 'DONE'
        : (!feedback.errors.length) ? 'PARTIALCORRECT' 
        : 'ERROR' 
    switch(state) {
        case 'DONE':
            return <Alert variant="success">All done!</Alert>;
        case 'PARTIALCORRECT':
            return <Alert variant="success">Correct, keep doing... (interactions left: {feedback.iterationsLeft})</Alert>;
        case 'ERROR':
            return feedback.errors.map(m => <Alert variant="danger">{m}</Alert>);
    }
}
