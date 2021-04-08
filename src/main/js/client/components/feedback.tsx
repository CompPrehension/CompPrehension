import { observer } from 'mobx-react';
import * as React from 'react';
import { Alert, Badge } from 'react-bootstrap';
import { exerciseStore } from "../stores/exercise-store";
import { Feedback as FeedbackType } from '../types/feedback';
import { Loader } from './loader';
import { Optional } from './optional';


export const Feedback = observer(() => {
    const { feedback, isFeedbackLoading } = exerciseStore;
    
    if (exerciseStore.isFeedbackLoading) {
        return <Loader />;
    }

    if (!feedback || exerciseStore.isQuestionLoading) {
        return null;
    }

    return (
        <div className="comp-ph-feedback-wrapper">            
            <p>
                {renderFeedback(feedback)}                
            </p>
            <p>
                <Optional condition={feedback.grade !== null}><Badge variant="primary">Grade: {feedback.grade}</Badge>{' '}</Optional>
                <Optional condition={feedback.totalSteps !== null}><Badge variant="success">Total steps: {feedback.totalSteps}</Badge>{' '}</Optional>
                <Optional condition={feedback.stepsWithErrors !== null && feedback.stepsWithErrors > 0}><Badge variant="danger">Steps with errors: {feedback.stepsWithErrors}</Badge>{' '}</Optional>
                <Optional condition={feedback.stepsLeft !== null}><Badge variant="info">Steps left: {feedback.stepsLeft}</Badge>{' '}</Optional>
            </p>              
        </div>
    );
});

const renderFeedback = (feedback: FeedbackType) => {
    const state = feedback.errors == null ? 'NONE' 
        : (!feedback.errors.length && feedback.stepsLeft === 0) ? 'DONE'
        : (!feedback.errors.length) ? 'PARTIALCORRECT' 
        : 'ERROR' 
    switch(state) {
        case 'DONE':
            return <Alert variant="success">All done!</Alert>;
        case 'PARTIALCORRECT':
            return <Alert variant="success">Correct, keep doing...</Alert>;
        case 'ERROR':
            return feedback.errors?.map(m => <Alert variant="danger">{m}</Alert>);
    }
}
