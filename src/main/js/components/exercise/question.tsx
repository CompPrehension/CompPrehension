import { observer } from "mobx-react";
import React, { useCallback } from "react";
import { QuestionStore } from "../../stores/question-store";
import { Loader } from "../common/loader";
import { Optional } from "../common/optional";
import { QuestionComponent } from "../common/question/question";
import { Feedback as FeedbackType, FeedbackMessage, FeedbackSuccessMessage } from '../../types/feedback';
import { Alert, Badge } from "react-bootstrap";
import { notNulAndUndefinded } from "../../utils/helpers";
import { GenerateSupQuestion } from "./generate-sup-question";
import { useTranslation } from "react-i18next";
import { Answer } from "../../types/answer";

type QuestionOptions = {
    store: QuestionStore, 
    showExtendedFeedback: boolean,
    onChanged?: (newHistory: Answer[]) => void,
}
export const Question = observer((props: QuestionOptions) => {
    const { store, showExtendedFeedback, onChanged:ParentOnChanged } = props;
    const questionData = store.question;
    if (store.isQuestionLoading) {
        return <div className="mt-2"><Loader /></div>;
    }
    if (!questionData) {
        return null;
    }    

    const onChanged = async (newHistory: Answer[]) => {
        if (await store.setFullAnswer(newHistory)) {
            ParentOnChanged?.(newHistory);
        }
    };
    const getAnswer = () => store.lastAnswer as Answer[];
    const getFeedback = () => store.feedback;

    return (
        <>
            <QuestionComponent question={questionData} answers={store.lastAnswer as Answer[]} getAnswers={getAnswer} onChanged={onChanged} getFeedback={getFeedback} isFeedbackLoading={store.isFeedbackLoading} isQuestionFreezed={store.isQuestionFreezed}/>
            <Feedback store={store} showExtendedFeedback={showExtendedFeedback}/>
        </>
    );
})

const Feedback = observer(({ store, showExtendedFeedback }: { store: QuestionStore, showExtendedFeedback: boolean }) => {
    const { feedback, isFeedbackLoading, isFeedbackVisible, isQuestionLoading, question } = store;
    const {t} = useTranslation();

    if (isFeedbackLoading) {
        return <div className="mt-2"><Loader /></div>;
    }

    if (!feedback || isQuestionLoading || !question) {
        return null;
    }

    const feedbackMessages = notNulAndUndefinded(feedback.stepsLeft) && feedback.stepsLeft === 0
        ? [{ type: 'SUCCESS', message: t('issolved_feeback') }] as FeedbackSuccessMessage[]
        : feedback.messages;

    return (
        <div className="comp-ph-feedback-wrapper mt-2">
            <Optional isVisible={isFeedbackVisible}>
                <div className="mb-3">
                    {feedbackMessages?.map((m) => 
                        <FeedbackAlert
                            message={m} 
                            showGenerateSupQuestion={showExtendedFeedback && question.options.showSupplementaryQuestions && 
                                m.type === 'ERROR' && m.violationLaw.canCreateSupplementaryQuestion} 
                        />)}                
                </div>
                <Optional isVisible={showExtendedFeedback}>
                    <div>
                        <Optional isVisible={feedback.grade !== null}><Badge variant="primary">{t('grade_feeback')}: {feedback.grade}</Badge>{' '}</Optional>
                        <Optional isVisible={feedback.correctSteps !== null}><Badge variant="success">{t('correctsteps_feeback')}: {feedback.correctSteps}</Badge>{' '}</Optional>
                        <Optional isVisible={notNulAndUndefinded(feedback.stepsWithErrors) && feedback.stepsWithErrors > 0}><Badge variant="danger">{t('stepswitherrors_feeback')}: {feedback.stepsWithErrors}</Badge>{' '}</Optional>
                        <Optional isVisible={notNulAndUndefinded(feedback.stepsLeft) && feedback.stepsLeft > 0}><Badge variant="info">{t('stepsleft_feeback')}: {feedback.stepsLeft}</Badge>{' '}</Optional>
                    </div>
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
            {
                showGenerateSupQuestion && message.type === 'ERROR' && message.violationLaw &&
                <GenerateSupQuestion violationLaw={message.violationLaw}/> || null
            }
        </Alert>
    )
})
