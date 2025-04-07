
import { observer } from 'mobx-react';
import * as React from 'react';
import { useState } from 'react';
import { Button } from 'react-bootstrap';
import { useTranslation } from "react-i18next";
import { container } from "tsyringe";
import { ExerciseStore } from "../../stores/exercise-store";


export const ExerciseGenerateNextAnswerBtn = observer(() => {    
    const [store] = useState(() => container.resolve(ExerciseStore));
    const { t } = useTranslation();
    if (!store.exercise?.options.correctAnswerGenerationEnabled) {
        return null;
    }

    const { exercise, currentAttempt, currentQuestion } = store;
    const { question, feedback } = currentQuestion;
    const isFeedbackLoading = currentQuestion.questionState === 'ANSWER_EVALUATING';
    const isQuestionLoading = currentQuestion.questionState === 'LOADING';
    if (!question || !exercise || !currentAttempt || isFeedbackLoading || isQuestionLoading || feedback?.stepsLeft === 0) {
        return null;
    }
    
    const onClicked = () => {
        store.currentQuestion.generateNextCorrectAnswer();
    };

    return (        
        <Button onClick={onClicked} variant="primary">{t('nextCorrectAnswerBtn')}</Button>
    )
})
