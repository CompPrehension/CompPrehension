
import { observer } from 'mobx-react';
import * as React from 'react';
import { useState } from 'react';
import { Button } from 'react-bootstrap';
import { container } from "tsyringe";
import { ExerciseStore } from "../../stores/exercise-store";
import { useTranslation } from "react-i18next";

export const GenerateNextAnswerBtn = observer(() => {    
    const [exerciseStore] = useState(() => container.resolve(ExerciseStore));
    const { t } = useTranslation();
    if (!exerciseStore.exercise?.options.correctAnswerGenerationEnabled) {
        return null;
    }    
    const { exercise, currentAttempt, currentQuestion } = exerciseStore;
    const { question, feedback } = currentQuestion;
    const isFeedbackLoading = currentQuestion.questionState === 'ANSWER_EVALUATING';
    const isQuestionLoading = currentQuestion.questionState === 'LOADING';
    if (!question || !exercise || !currentAttempt || isFeedbackLoading || isQuestionLoading || feedback?.stepsLeft === 0) {
        return null;
    }

    const onClicked = () => {
        exerciseStore.currentQuestion.generateNextCorrectAnswer();
    };

    return (        
        <Button onClick={onClicked} variant="primary">{t('nextCorrectAnswerBtn')}</Button>
    )
})
