import {observer} from "mobx-react";
import React from "react";
import {Alert, Badge} from "react-bootstrap";
import {useTranslation} from "react-i18next";
import {QuestionStore} from "../../stores/question-store";
import {SupplementaryQuestionStore} from "../../stores/sup-question-store";
import {FeedbackMessage, FeedbackSuccessMessage} from "../../types/feedback";
import {isNullOrUndefined} from "../../utils/helpers";
import {Loader} from "../common/loader";
import {GenerateSupQuestion} from "./generate-sup-question";

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

    const defaultFeedbackMessage: FeedbackSuccessMessage = { type: 'SUCCESS',
         message: t('issolved_feeback'), violationLaws: [] };

    const feedbackMessages = feedback.messages;
    if (feedbackMessages !== null && store.questionState === 'COMPLETED') {
        feedbackMessages?.push(defaultFeedbackMessage);
    }

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
                                    m.type === 'ERROR' && m.violationLaws?.every(e => e.canCreateSupplementaryQuestion)} 
                            />)}                
                    </div>
                    {showExtendedFeedback && 
                        <div>
                            {feedback.grade !== null && <><Badge variant="primary">{t('grade_feeback')}: {feedback.grade}</Badge>{' '}</>}
                            {feedback.correctSteps !== null && <><Badge variant="success">{t('correctsteps_feeback')}: {feedback.correctSteps}</Badge>{' '}</>}
                            {!isNullOrUndefined(feedback.stepsWithErrors) && feedback.stepsWithErrors > 0 && <><Badge variant="danger">{t('stepswitherrors_feeback')}: {feedback.stepsWithErrors}</Badge>{' '}</>}
                            {!isNullOrUndefined(feedback.stepsLeft) && feedback.stepsLeft > 0 && <><Badge variant="info">{t('stepsleft_feeback')}: {feedback.stepsLeft}</Badge>{' '}</>}
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
            <div data-domain-laws={message.violationLaws?.map(v => v.name).join(";")}
                dangerouslySetInnerHTML={{ __html: message.message }} 
                />
            {showGenerateSupQuestion && message.type === 'ERROR' && message.violationLaws &&
                <GenerateSupQuestion 
                    store={supQuestionStore!}
                    violationLaw={message.violationLaws}/> || null
            }
        </Alert>
    )
})

