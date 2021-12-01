import { observer } from 'mobx-react';
import * as React from 'react';
import { useState } from 'react';
import { Alert, Button } from 'react-bootstrap';
import { container } from "tsyringe";
import { ExerciseStore } from "../../stores/exercise-store";
import { QuestionStore } from '../../stores/question-store';
import { delayPromise } from '../../utils/helpers';
import { Modal } from '../common/modal';
import { Optional } from '../common/optional';
import { Question } from './question';
import { useTranslation } from "react-i18next";
import { FeedbackViolationLaw } from '../../types/feedback';

export const GenerateSupQuestion = observer(({ violationLaw } : { violationLaw: FeedbackViolationLaw }) => {
    const [exerciseStore] = useState(() => container.resolve(ExerciseStore));
    const [questionStore] = useState(() => container.resolve(QuestionStore));
    const { t } = useTranslation();
    const [isModalVisible, setIsModalVisible] = useState(false);
    const [isButtonsVisible, setIsButtonsVisible] = useState(true);
    const [isAllVisible, setAllVisible] = useState(true);
    if (!exerciseStore.sessionInfo?.exercise.options.supplementaryQuestionsEnabled || 
        violationLaw.canCreateSupplementaryQuestion === false) {
        return null;
    }

    const onDetailsClicked = async () => { 
        setIsButtonsVisible(false);  
        setIsModalVisible(true);
        if (!exerciseStore.currentAttempt?.attemptId || !exerciseStore.currentQuestion.question) {
            return;
        }
        await questionStore.generateSupplementaryQuestion(exerciseStore.currentAttempt.attemptId, exerciseStore.currentQuestion.question?.questionId, [violationLaw].map(v => v.name));
        if (!questionStore.question) {
            console.log(`no need to generate sup question`);
            setAllVisible(false);
        }
    }
    const onGotitClicked = () => {
        setAllVisible(false);
    }
    const OnAnswered = async () => {
        try {
            questionStore.isQuestionFreezed = true;

            console.log(`show feedback for 3 seconds`);
            await delayPromise(3000);
            //console.log(`hide feedback and wait for 1 seconds`);        
            //questionStore.isFeedbackVisible = false;
            //await delayPromise(1000);

            const newViolationLaw = questionStore.feedback?.messages && questionStore.feedback.messages[0].violationLaw || null;
            if (!newViolationLaw) {
                console.log(`empty violation laws`);
                setAllVisible(false);
                return;            
            }
            if (!exerciseStore.currentAttempt?.attemptId || !exerciseStore.currentQuestion.question) {
                console.log(`problems with attempt`);
                return;
            }
            await questionStore.generateSupplementaryQuestion(exerciseStore.currentAttempt.attemptId, exerciseStore.currentQuestion.question?.questionId, [newViolationLaw].map(v => v.name));
            if (!questionStore.question) {
                console.log(`no need to generate sup question`);
                // remove redurant feedback
                //if (!!exerciseStore.currentQuestion.feedback?.messages) {
                //    exerciseStore.currentQuestion.feedback.messages = exerciseStore.currentQuestion.feedback.messages.filter(m => m.type !== 'ERROR' || m.violationLaws?.[0] !== violationLaws?.[0]);
                //}
                setAllVisible(false);
            }
        } finally {
            questionStore.isQuestionFreezed = false;
        }
    }

    return (
        <Optional isVisible={isAllVisible}>
            <Optional isVisible={isButtonsVisible}>
                <div className="d-flex flex-row mt-3">
                    <Button onClick={onDetailsClicked} variant="primary">{t('generateSupQuestion_details')}</Button>
                    <Button onClick={onGotitClicked} variant="success" className="ml-2">{t('generateSupQuestion_gotit')}</Button>
                </div>
            </Optional>            
            <Modal  type={'DIALOG'} size={'xl'} 
                    show={isModalVisible}
                    closeButton={false} 
                    handleClose={() => setIsModalVisible(false)}>
                <Question store={questionStore} showExtendedFeedback={false} onChanged={OnAnswered}/>                
                {questionStore.storeState.tag === 'ERROR' &&
                    <div className="mt-2"><Alert variant='danger'>{questionStore.storeState.error.message}</Alert></div>}
            </Modal>
        </Optional>
    )
})
