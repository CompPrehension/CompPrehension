
import { observer } from 'mobx-react';
import * as React from 'react';
import { useState } from 'react';
import { Button } from 'react-bootstrap';
import { getExerciseStore } from "../../stores/exercise-store";
import { useTranslation } from "react-i18next";
import { Modal } from '../common/modal';

export const GenerateNextQuestionBtn = observer(() => {
    const exerciseStore = getExerciseStore();
    const { t } = useTranslation();
    const [isModalVisible, setIsModalVisible] = useState(false);
    
    const { exercise, currentAttempt } = exerciseStore;
    const { question } = exerciseStore.currentQuestion;
    const isFeedbackLoading = exerciseStore.currentQuestion.questionState === 'ANSWER_EVALUATING';
    const isQuestionLoading = exerciseStore.currentQuestion.questionState === 'LOADING';
    if (!question || !exercise || !currentAttempt || isQuestionLoading || isFeedbackLoading) {
        return null;
    }

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
            <Button onClick={onClicked} variant="primary" className='comp-ph-next-question-btn'>{t('generateNextQuestion_nextQuestion')}</Button>
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

