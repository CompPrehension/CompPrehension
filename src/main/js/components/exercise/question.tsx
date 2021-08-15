import { observer } from "mobx-react";
import React, { useCallback } from "react";
import { QuestionStore } from "../../stores/question-store";
import { Loader } from "../common/loader";
import { Optional } from "../common/optional";
import { QuestionComponent } from "../common/question/question";
import { Feedback as FeedbackType, FeedbackMessage } from '../../types/feedback';
import { Alert, Badge } from "react-bootstrap";
import { notNullOrUndefinded } from "../../utils/helpers";
import { GenerateSupQuestion } from "./generate-sup-question";
import { useTranslation } from "react-i18next";

type QuestionOptions = {
    store: QuestionStore, 
    showExtendedFeedback: boolean,
    onChanged?: (newHistory: [number, number][]) => void,
}
export const Question = observer((props: QuestionOptions) => {
    const { store, showExtendedFeedback, onChanged:ParentOnChanged } = props;
    const questionData = store.question;
    if (store.isQuestionLoading) {
        return <Loader />;
    }
    if (!questionData) {
        return null;
    }    

    const onChanged = async (newHistory: [number, number][]) => {
        if (store.isHistoryChanged(newHistory)) {
            await store.updateAnswersHistory(newHistory);
            ParentOnChanged?.(newHistory);
        }
    };
    const getAnswers = () => store.answersHistory;

    return (
        <>
            <QuestionComponent question={questionData} answers={store.answersHistory} getAnswers={getAnswers} onChanged={onChanged} feedback={store.feedback}/>
            <Feedback store={store} showExtendedFeedback={showExtendedFeedback}/>
        </>
    );
})

const Feedback = observer(({ store, showExtendedFeedback }: { store: QuestionStore, showExtendedFeedback: boolean }) => {
    const { feedback, isFeedbackLoading, isFeedbackVisible, isQuestionLoading, question } = store;
    const { t } = useTranslation();

    if (isFeedbackLoading) {
        return <Loader />;
    }

    if (!feedback || isQuestionLoading || !question) {
        return null;
    }

    return (
        <div className="comp-ph-feedback-wrapper">
            <Optional isVisible={isFeedbackVisible}>
                <p>
                    {feedback.messages?.map(m => <FeedbackAlert message={m} showGenerateSupQuestion={showExtendedFeedback && question.options.showSupplementaryQuestions} />)}                
                </p>
                <Optional isVisible={showExtendedFeedback}>
                    <p>
                        <Optional isVisible={feedback.grade !== null}><Badge variant="primary">{t('grade_feeback')}: {feedback.grade}</Badge>{' '}</Optional>
                        <Optional isVisible={feedback.correctSteps !== null}><Badge variant="success">{t('correctsteps_feeback')}: {feedback.correctSteps}</Badge>{' '}</Optional>
                        <Optional isVisible={notNullOrUndefinded(feedback.stepsWithErrors) && feedback.stepsWithErrors > 0}><Badge variant="danger">{t('stepswitherrors_feeback')}: {feedback.stepsWithErrors}</Badge>{' '}</Optional>
                        <Optional isVisible={notNullOrUndefinded(feedback.stepsLeft) && feedback.stepsLeft > 0}><Badge variant="info">{t('stepsleft_feeback')}: {feedback.stepsLeft}</Badge>{' '}</Optional>                
                    </p>
                </Optional>
            </Optional>            
        </div>
    );
});

const FeedbackAlert = observer(({ message, showGenerateSupQuestion }: { message: FeedbackMessage, showGenerateSupQuestion: boolean }) => {
    const variant = message.type === 'SUCCESS' ? 'success' : 'danger';
    return(
        <Alert variant={variant}>
            {message.message}
            <Optional isVisible={showGenerateSupQuestion}>
                <GenerateSupQuestion violationLaws={message.type === 'ERROR' && message.violationLaws || []}/>
            </Optional>
        </Alert>
    )
})
