import { observer } from "mobx-react";
import React from "react";
import { QuestionStore } from "../../stores/question-store";
import { Loader } from "../common/loader";
import { Optional } from "../common/optional";
import { QuestionComponent } from "../common/question/question";
import { Feedback as FeedbackType, FeedbackMessage } from '../../types/feedback';
import { Alert, Badge } from "react-bootstrap";
import { notNullOrUndefinded } from "../../utils/helpers";
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

    return (
        <div className="comp-ph-feedback-wrapper">            
            <p>
                {feedback.messages?.map(m => <FeedbackAlert message={m} showGenerateSupQuestion={showExtendedFeedback} />)}                
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

const FeedbackAlert = ({ message, showGenerateSupQuestion }: { message: FeedbackMessage, showGenerateSupQuestion: boolean }) => {
    const variant = message.type === 'SUCCESS' ? 'success' : 'danger';
    return(
        <Alert variant={variant}>
            {message.message}
            <Optional isVisible={showGenerateSupQuestion}>
                <GenerateSupQuestion violations={message.type === 'ERROR' && message.violations || []}/>
            </Optional>
        </Alert>
    )
}
