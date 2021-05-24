import { observer } from "mobx-react";
import React from "react";
import { QuestionStore } from "../../stores/question-store";
import { Loader } from "../common/loader";
import { Optional } from "../common/optional";
import { QuestionComponent } from "../common/question/question";
import { Feedback as FeedbackType, FeedbackMessage } from '../../types/feedback';
import { Alert, Badge } from "react-bootstrap";
import { toJS } from "mobx";
import { notNullOrUndefinded } from "../../utils/helpers";
import { boolean } from "io-ts";
import { GenerateSupQuestion } from "./generate-sup-question";

export const Question = observer(({ store, showExtendedFeedback }: { store: QuestionStore, showExtendedFeedback: boolean }) => {
    const questionData = store.question;
    if (store.isQuestionLoading) {
        return <Loader />;
    }
    if (!questionData) {
        return null;
    }    

    const onChanged = (newHistory: [number, number][]) => {
        if (store.isHistoryChanged(newHistory)) {
            store.updateAnswersHistory(newHistory);
        }
    }

    return (
        <>
            <QuestionComponent question={questionData} answers={store.answersHistory} getAnswers={() => store.answersHistory} onChanged={onChanged} />
            <Feedback store={store} showExtendedFeedback={showExtendedFeedback}/>
        </>
    );
})

const Feedback = observer(({ store, showExtendedFeedback }: { store: QuestionStore, showExtendedFeedback: boolean }) => {
    const { feedback, isFeedbackLoading, isQuestionLoading } = store;
    
    if (isFeedbackLoading) {
        return <Loader />;
    }

    if (!feedback || isQuestionLoading) {
        return null;
    }

    const renderFeedback = (feedback: FeedbackType) => {
        if (!feedback.message) {
            return null;
        }
        const variant = feedback.message.messageType === 'SUCCESS' ? 'success' : 'danger';
        return feedback.message.strings?.map(m => <Alert variant={variant}>{m}</Alert>);
    }

    return (
        <div className="comp-ph-feedback-wrapper">            
            <p>
                {renderFeedback(feedback)}                
            </p>
            <Optional isVisible={showExtendedFeedback}>
                <p>
                    <Optional isVisible={feedback.grade !== null}><Badge variant="primary">Grade: {feedback.grade}</Badge>{' '}</Optional>
                    <Optional isVisible={feedback.correctSteps !== null}><Badge variant="success">Correct steps: {feedback.correctSteps}</Badge>{' '}</Optional>
                    <Optional isVisible={notNullOrUndefinded(feedback.stepsWithErrors) && feedback.stepsWithErrors > 0}><Badge variant="danger">Steps with errors: {feedback.stepsWithErrors}</Badge>{' '}</Optional>
                    <Optional isVisible={notNullOrUndefinded(feedback.stepsLeft) && feedback.stepsLeft > 0}><Badge variant="info">Steps left: {feedback.stepsLeft}</Badge>{' '}</Optional>                
                </p>
            </Optional>
        </div>
    );
});
