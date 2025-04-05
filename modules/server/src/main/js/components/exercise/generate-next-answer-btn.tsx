
import { observer } from 'mobx-react';
import * as React from 'react';
import { useState } from 'react';
import { Button } from 'react-bootstrap';
import { container } from "tsyringe";
import { ExerciseStore } from "../../stores/exercise-store";
import { useTranslation } from "react-i18next";
import { QuestionStore } from '../../stores/question-store';

type GenerateNextAnswerBtnProps = {
    explicitQuestionStore?: QuestionStore;
};

export const GenerateNextAnswerBtn = observer(({ explicitQuestionStore }: GenerateNextAnswerBtnProps) => {    
    const [store] = useState(() => explicitQuestionStore == undefined ? 
        container.resolve(ExerciseStore) : explicitQuestionStore);
    const { t } = useTranslation();
    if (store instanceof ExerciseStore && !store.exercise?.options.correctAnswerGenerationEnabled) {
        return null;
    }

    if (store instanceof ExerciseStore) {
        const { exercise, currentAttempt, currentQuestion } = store;
        const { question, feedback } = currentQuestion;
        const isFeedbackLoading = currentQuestion.questionState === 'ANSWER_EVALUATING';
        const isQuestionLoading = currentQuestion.questionState === 'LOADING';
        if (!question || !exercise || !currentAttempt || isFeedbackLoading || isQuestionLoading || feedback?.stepsLeft === 0) {
            return null;
        }
    }
    
    const onClicked = () => {
        if (store instanceof ExerciseStore) {
            store.currentQuestion.generateNextCorrectAnswer();
        } else {
            store.generateNextCorrectAnswer();
        }
    };

    return (        
        <Button onClick={onClicked} variant="primary">{t('nextCorrectAnswerBtn')}</Button>
    )
})
