
import { observer } from 'mobx-react';
import * as React from 'react';
import { Button } from 'react-bootstrap';
import { useTranslation } from "react-i18next";
import { QuestionStore } from '../../stores/question-store';

type GenerateNextAnswerBtnProps = {
    questionStore: QuestionStore;
};

export const QuestionGenerateNextAnswerBtn = observer(({ questionStore }: GenerateNextAnswerBtnProps) => {    
    const { t } = useTranslation();
    
    const onClicked = () => {
        questionStore.generateNextCorrectAnswer();
    };

    return (        
        <Button onClick={onClicked} variant="primary">{t('nextCorrectAnswerBtn')}</Button>
    )
})
