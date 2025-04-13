
import { observer } from 'mobx-react';
import * as React from 'react';
import { Button } from 'react-bootstrap';
import { useTranslation } from "react-i18next";
import { QuestionStore } from '../../stores/question-store';

type GenerateNextAnswerBtnProps = {
    store: QuestionStore;
};

export const GenerateNextAnswerBtn = observer(({ store }: GenerateNextAnswerBtnProps) => {    
    const { t } = useTranslation();

    const { question, feedback } = store;
    const isFeedbackLoading = store.questionState === 'ANSWER_EVALUATING';
    const isQuestionLoading = store.questionState === 'LOADING';
    if (!question || isFeedbackLoading || isQuestionLoading || feedback?.stepsLeft === 0) {
        return null;
    }
    
    const onClicked = () => {
        store.generateNextCorrectAnswer();
    };

    return (        
        <Button onClick={onClicked} variant="primary">{t('nextCorrectAnswerBtn')}</Button>
    )
})
