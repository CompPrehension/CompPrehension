import { observer } from "mobx-react";
import React from "react";
import { Alert, Button } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import { QuestionStore } from "../../stores/question-store";

type ClarificationProps = {
    store: QuestionStore,
}

/** Asks why the student chose an answer that several hypotheses explain; the question stays frozen until then. */
export const Clarification = observer(({ store }: ClarificationProps) => {
    const { t } = useTranslation();
    const clarification = store.pendingFeedback?.clarification;

    if (clarification) {
        return (
            <Alert variant='warning' className='comp-ph-clarification'>
                <div className='mb-2 fw-semibold'>{clarification.prompt}</div>
                <div className='d-flex flex-column align-items-start gap-2'>
                    {clarification.options.map(option =>
                        <Button key={option.hypothesis} variant='outline-dark' size='sm' className='text-start'
                                disabled={store.isClarificationSending}
                                onClick={() => store.answerClarification(option.hypothesis)}>
                            <div dangerouslySetInnerHTML={{ __html: option.reason }}></div>
                        </Button>
                    )}
                    <Button variant='outline-secondary' size='sm' disabled={store.isClarificationSending}
                            onClick={() => store.answerClarification(null)}>
                        {t('clarification_other_reason')}
                    </Button>
                </div>
            </Alert>
        );
    }

    return null;
});
