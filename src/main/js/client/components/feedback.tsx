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

    return (
        <div className="comp-ph-feedback-wrapper">
            <p>Current grade: {feedback.grade}</p>
            <p>
                {feedback.errors.length
                    ? feedback.errors.map(m => <Alert variant="danger">{m}</Alert>)
                    : <Alert variant="success">{"Correct, keep doing..."}</Alert>}
            </p>            
        </div>
    );
});
