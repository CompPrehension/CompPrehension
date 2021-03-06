import { observer } from 'mobx-react';
import * as React from 'react';
import { Alert, Spinner } from 'react-bootstrap';
import store from '../store';
import { Loader } from './loader';


export const Feedback = observer(() => {
    const { feedback, isFeedbackLoading } = store;
    
    if (store.isFeedbackLoading) {
        return <Loader />;
    }

    if (feedback === undefined) {
        return null;
    }

    if (feedback.errors.length === 0) {
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
            {feedback.errors.map(m => <Alert variant="danger">{m}</Alert>)}
        </div>
    );
});
