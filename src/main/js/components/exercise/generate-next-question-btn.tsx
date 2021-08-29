
import { observer } from 'mobx-react';
import * as React from 'react';
import { useCallback, useState } from 'react';
import { Button } from 'react-bootstrap';
import { container } from "tsyringe";
import { ExerciseStore } from "../../stores/exercise-store";
import { useTranslation } from "react-i18next";
import { Modal } from '../common/modal';

export const GenerateNextQuestionBtn = observer(() => {
    const [exerciseStore] = useState(() => container.resolve(ExerciseStore));
    const { t } = useTranslation();
    const [isModalVisible, setIsModalVisible] = useState(false);
    if (!exerciseStore.sessionInfo?.exercise.options.newQuestionGenerationEnabled) {
        return null;
    }
    
    const { sessionInfo, currentAttempt } = exerciseStore;
    const { question, isQuestionLoading, isFeedbackLoading } = exerciseStore.currentQuestion;
    if (!question || !sessionInfo || !currentAttempt || isQuestionLoading || isFeedbackLoading) {
        return null;
    }
    
    /*
    const { user } = sessionInfo;
    const { questionIds } = currentAttempt;

    
    // show btn if user is admin or if he is on the last question and has finished it
    if (!user.roles.includes('ADMIN') && !user.roles.includes('TEACHER') &&
        (questionIds[questionIds.length - 1] !== question.questionId || exerciseStore.currentQuestion.feedback?.stepsLeft !== 0)) {
        return null;
    }
    */

    const onModalClosed = () => {
        setIsModalVisible(false);
    }
    const onClicked = async () => {
        const { questionIds=[] } = currentAttempt;
        const currentQuestionIdx = questionIds.indexOf(question.questionId);
        const isLastQuestion = currentQuestionIdx === questionIds.length - 1;
        if (exerciseStore.currentQuestion.feedback?.stepsLeft !== 0 && isLastQuestion) {
            setIsModalVisible(true);
        } else {
            await generateOrLoadQuestion();
        }
    }
    const generateOrLoadQuestion = async () => {
        setIsModalVisible(false);
        const { questionIds=[] } = currentAttempt;
        const currentQuestionIdx = questionIds.indexOf(question.questionId);
        if (currentQuestionIdx === questionIds.length - 1) {
            await exerciseStore.generateQuestion();
        } else {
            await exerciseStore.currentQuestion.loadQuestion(questionIds[currentQuestionIdx + 1]);
        }        
    }

    return (
        <>
            <Button onClick={onClicked} variant="primary" >{t('generateNextQuestion_nextQuestion')}</Button>
            <Modal show={isModalVisible}
                   title={t('generateNextQuestion_warning')}
                   type='MODAL'
                   size='lg'
                   primaryBtnTitle={t('generateNextQuestion_continueAttempt')}
                   handlePrimaryBtnClicked={onModalClosed}
                   secondaryBtnTitle={t('generateNextQuestion_nextQuestion')}
                   handleSecondaryBtnClicked={generateOrLoadQuestion}
                   closeButton={false} 
                   handleClose={onModalClosed}>
                <div>{t('generateNextQuestion_modalMessage1')}</div>
                <div>{t('generateNextQuestion_modalMessage2')}</div>
            </Modal>
        </>
    )
})

