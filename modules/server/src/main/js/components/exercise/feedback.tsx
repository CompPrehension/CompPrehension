import React from "react";
import {QuestionStore} from "../../stores/question-store";
import {Loader} from "../common/loader";
import {FeedbackMessage, FeedbackSuccessMessage} from "../../types/feedback";
import {SupplementaryQuestionStore} from "../../stores/sup-question-store";
import {GenerateSupQuestion} from "./generate-sup-question";
import {observer} from "mobx-react";
import {Alert, Badge} from "react-bootstrap";
import {useTranslation} from "react-i18next";
import {isNullOrUndefined} from "../../utils/helpers";
import {ParsedMessage} from "./domain-terms";
import {Clarification} from "./clarification";


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

    if (isQuestionLoading || !question) {
        return null;
    }

    if (store.isQuestionFreezed) {
        return (
            <div className='comp-ph-feedback-wrapper mt-3'>
                <Clarification store={store} />
            </div>
        );
    }

    if (!feedback) {
        return null;
    }

    const defaultFeedbackMessage: FeedbackSuccessMessage = { type: 'SUCCESS',
         message: t('issolved_feeback'), knowledge: [] };

    // The explanation of the named reason already says whether the answer is right, so it replaces the messages.
    const answerMessages: FeedbackMessage[] | null | undefined = store.clarificationExplanation
        ? [{
            type: feedback.isCorrect ? 'SUCCESS' : 'ERROR',
            message: store.clarificationExplanation,
            knowledge: feedback.messages?.flatMap(m => m.knowledge ?? []) ?? null,
        }]
        : feedback.messages;
    // A new list rather than a push: changing observable feedback while rendering re-renders forever.
    const feedbackMessages = store.questionState === 'COMPLETED'
        ? [...(answerMessages ?? []), defaultFeedbackMessage]
        : answerMessages;

    return (
      <div className='comp-ph-feedback-wrapper mt-3'>
        {isFeedbackVisible && (
          <>
            <div className='mb-3'>
              {feedbackMessages?.map((m, i) => (
                <FeedbackAlert
                  key={i}
                  message={m}
                  supQuestionStore={store.supplementaryQuestion}
                  showGenerateSupQuestion={
                    showExtendedFeedback &&
                    question.options.showSupplementaryQuestions &&
                    m.type === 'ERROR' &&
                    m.knowledge?.every(
                      (e) => e.canCreateSupplementaryQuestion
                    )
                  }
                />
              ))}
            </div>
            {showExtendedFeedback && (
              <div>
                {feedback.grade !== null && (
                  <>
                    <Badge
                      className='comp-ph-feedback-grade'
                      bg='primary'
                    >
                      {t('grade_feeback')}: {feedback.grade}
                    </Badge>{' '}
                  </>
                )}
                {feedback.correctSteps !== null && (
                  <>
                    <Badge bg='success'>
                      {t('correctsteps_feeback')}: {feedback.correctSteps}
                    </Badge>{' '}
                  </>
                )}
                {!isNullOrUndefined(feedback.stepsWithErrors) &&
                  feedback.stepsWithErrors > 0 && (
                    <>
                      <Badge
                        className='comp-ph-feedback-error-steps'
                        bg='danger'
                      >
                        {t('stepswitherrors_feeback')}:{' '}
                        {feedback.stepsWithErrors}
                      </Badge>{' '}
                    </>
                  )}
                {!isNullOrUndefined(feedback.stepsLeft) &&
                  feedback.stepsLeft > 0 && (
                    <>
                      <Badge className='comp-ph-feedback-remaining-steps'
                      bg='info'>
                        {t('stepsleft_feeback')}: {feedback.stepsLeft}
                      </Badge>{' '}
                    </>
                  )}
              </div>
            )}
          </>
        )}
      </div>
    );
});

type FeedbackAlertProps = {    
    message: FeedbackMessage,
    showGenerateSupQuestion?: boolean
    supQuestionStore?: SupplementaryQuestionStore,
}
export const FeedbackAlert = observer((props: FeedbackAlertProps) => {
    const { supQuestionStore, message } = props;
    const showGenerateSupQuestion = props.showGenerateSupQuestion && supQuestionStore != undefined;

    const variant = message.type === 'SUCCESS' ? 'success' : 'danger';
    return (
      <Alert variant={variant} className={variant === 'danger' ? 'comp-ph-feedback-error' : 'comp-ph-feedback-success'}>
        <div
          data-domain-knowledge={message.knowledge?.map((v) => v.name).join(';')}
        >
          <ParsedMessage html={message.message} />
        </div>
        {(showGenerateSupQuestion &&
          message.type === 'ERROR' &&
          message.knowledge && (
            <GenerateSupQuestion
              store={supQuestionStore!}
              knowledge={message.knowledge}
            />
          )) ||
          null}
      </Alert>
    );
})
