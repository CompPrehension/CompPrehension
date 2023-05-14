import React from "react";
import { QuestionStore } from "../../stores/question-store";
import { Loader } from "../common/loader";
import { FeedbackMessage, FeedbackSuccessMessage } from "../../types/feedback";
import { SupplementaryQuestionStore } from "../../stores/sup-question-store";
import { GenerateSupQuestion } from "./generate-sup-question";
import { observer } from "mobx-react";
import { Alert, Badge } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import { notNulAndUndefinded } from "../../utils/helpers";

type FeedbackProps = { 
    store: QuestionStore,
    showExtendedFeedback: boolean,
}
export const Feedback = observer(({ store, showExtendedFeedback }: FeedbackProps) => {
    const { feedback, isFeedbackVisible, question } = store;
    const isFeedbackLoading = store.questionState === 'ANSWER_EVALUATING';
    const isQuestionLoading = store.questionState === 'LOADING';
    const {t} = useTranslation();

    if (isFeedbackLoading) {
        return <div className="mt-2"><Loader /></div>;
    }

    if (!feedback || isQuestionLoading || !question) {
        return null;
    }

    const feedbackMessages = store.questionState === 'COMPLETED'
        ? [{ type: 'SUCCESS', message: t('issolved_feeback') }] as FeedbackSuccessMessage[]
        : feedback.messages;

    return (
        <div className="comp-ph-feedback-wrapper mt-2">
            {isFeedbackVisible && 
                <>
                    <div className="mb-3">
                        {feedbackMessages?.map((m) => 
                            <FeedbackAlert                            
                                message={m}
                                supQuestionStore={store.supplementaryQuestion}
                                showGenerateSupQuestion={showExtendedFeedback && question.options.showSupplementaryQuestions && 
                                    m.type === 'ERROR' && m.violationLaw.canCreateSupplementaryQuestion} 
                            />)}                
                    </div>
                    {showExtendedFeedback && 
                        <div>
                            {feedback.grade !== null && <><Badge variant="primary">{t('grade_feeback')}: {feedback.grade}</Badge>{' '}</>}
                            {feedback.correctSteps !== null && <><Badge variant="success">{t('correctsteps_feeback')}: {feedback.correctSteps}</Badge>{' '}</>}
                            {notNulAndUndefinded(feedback.stepsWithErrors) && feedback.stepsWithErrors > 0 && <><Badge variant="danger">{t('stepswitherrors_feeback')}: {feedback.stepsWithErrors}</Badge>{' '}</>}
                            {notNulAndUndefinded(feedback.stepsLeft) && feedback.stepsLeft > 0 && <><Badge variant="info">{t('stepsleft_feeback')}: {feedback.stepsLeft}</Badge>{' '}</>}
                        </div>
                    }
                </>
            }          
        </div>
    );
});

type FeedbackAlertProps = {    
    message: FeedbackMessage,
    showGenerateSupQuestion?: boolean
    supQuestionStore?: SupplementaryQuestionStore,
}
export const FeedbackAlert = observer((props: FeedbackAlertProps) => {
    let { supQuestionStore, message, showGenerateSupQuestion } = props;    
    showGenerateSupQuestion = showGenerateSupQuestion && supQuestionStore != undefined;

    const variant = message.type === 'SUCCESS' ? 'success' : 'danger';
    return(
        <Alert variant={variant}>
            {message.message}            
            {showGenerateSupQuestion && message.type === 'ERROR' && message.violationLaw &&
                <GenerateSupQuestion 
                    store={supQuestionStore!}
                    violationLaw={message.violationLaw}/> || null
            }
        </Alert>
    )
})

