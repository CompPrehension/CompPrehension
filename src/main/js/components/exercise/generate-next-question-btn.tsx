
import { observer } from 'mobx-react';
import * as React from 'react';
import { useState } from 'react';
import { Button } from 'react-bootstrap';
import { container } from "tsyringe";
import { ExerciseStore } from "../../stores/exercise-store";
import { useTranslation } from "react-i18next";

export const GenerateNextQuestionBtn = observer(() => {
    const [exerciseStore] = useState(() => container.resolve(ExerciseStore));
    const { t } = useTranslation();
    if (!exerciseStore.sessionInfo?.exercise.options.newQuestionGenerationEnabled) {
        return null;
    }
    
    const { sessionInfo, currentAttempt } = exerciseStore;
    const { question } = exerciseStore.currentQuestion;
    if (!question || !sessionInfo || !currentAttempt) {
        return null;
    }
    const onClicked = async () => {
        const { questionIds=[] } = currentAttempt;
        const currentQuestionIdx = questionIds.indexOf(question.questionId);
        if (currentQuestionIdx === questionIds.length - 1) {
            await exerciseStore.generateQuestion();
        } else {
            await exerciseStore.currentQuestion.loadQuestion(questionIds[currentQuestionIdx + 1]);
        }        
    };

    const { user } = sessionInfo;
    const { questionIds } = currentAttempt;

    // show btn if user is admin or if he is on the last question and has finished it
    if (!user.roles.includes('ADMIN') && !user.roles.includes('TEACHER') &&
        (questionIds[questionIds.length - 1] !== question.questionId || exerciseStore.currentQuestion.feedback?.stepsLeft !== 0)) {
        return null;
    }

    return (        
        <Button onClick={onClicked} variant="primary" >{t('generateNextQuestionBtn')}</Button>
    )
})

