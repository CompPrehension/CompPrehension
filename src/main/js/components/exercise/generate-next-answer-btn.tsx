
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
    if (!exerciseStore.sessionInfo?.exercise.options.correctAnswerGenerationEnabled) {
        return null;
    }    
    const { sessionInfo, currentAttempt, currentQuestion } = exerciseStore;
    const { question, isFeedbackLoading, feedback, isQuestionLoading } = currentQuestion;
    if (!question || !sessionInfo || !currentAttempt || isFeedbackLoading || isQuestionLoading || feedback?.stepsLeft === 0) {
        return null;
    }

    const onClicked = () => {
        exerciseStore.currentQuestion.generateNextCorrectAnswer();
    };

    return (        
        <Button onClick={onClicked} variant="primary">{t('nextCorrectAnswerBtn')}</Button>
    )
})
