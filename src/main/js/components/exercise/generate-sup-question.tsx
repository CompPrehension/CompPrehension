import { observer } from 'mobx-react';
import * as React from 'react';
import { useState } from 'react';
import { Button } from 'react-bootstrap';
import { container } from "tsyringe";
import { ExerciseStore } from "../../stores/exercise-store";
import { QuestionStore } from '../../stores/question-store';
import { delayPromise } from '../../utils/helpers';
import { Modal } from '../common/modal';
import { Optional } from '../common/optional';
import { Question } from './question';
import { useTranslation } from "react-i18next";

export const GenerateSupQuestion = observer(({ violationLaws } : { violationLaws: string[] }) => {
    const [exerciseStore] = useState(() => container.resolve(ExerciseStore));
    const [questionStore] = useState(() => container.resolve(QuestionStore));
    const { t } = useTranslation();
    const [isModalVisible, setIsModalVisible] = useState(false);
    const [isButtonsVisible, setIsButtonsVisible] = useState(true);
    const [isAllVisible, setAllVisible] = useState(true);
    if (!exerciseStore.sessionInfo?.exercise.options.supplementaryQuestionsEnabled) {
        return null;
    }

    const onDetailsClicked = async () => { 
        setIsButtonsVisible(false);  
        setIsModalVisible(true);
        if (!exerciseStore.currentAttempt?.attemptId || !violationLaws.length || !exerciseStore.currentQuestion.question) {
            return;
        }
        await questionStore.generateSupplementaryQuestion(exerciseStore.currentAttempt.attemptId, exerciseStore.currentQuestion.question?.questionId, violationLaws);
        if (!questionStore.question) {
            console.log(`no need to generate sup question`);
            setAllVisible(false);
        }
    }
    const onGotitClicked = () => {
        setAllVisible(false);
    }
    const OnAnswered = async () => {
        console.log(`show feedback for 3 seconds`);
        await delayPromise(3000);
        //console.log(`hide feedback and wait for 1 seconds`);        
        //questionStore.isFeedbackVisible = false;
        //await delayPromise(1000);

        const newViolationLaws = questionStore.feedback?.messages && questionStore.feedback.messages?.[0].violationLaws || [];
        if (!newViolationLaws.length) {
            console.log(`empty violation laws`);
            setAllVisible(false);
            return;            
        }
        if (!exerciseStore.currentAttempt?.attemptId || !exerciseStore.currentQuestion.question) {
            console.log(`problems with attempt`);
            return;
        }
        await questionStore.generateSupplementaryQuestion(exerciseStore.currentAttempt.attemptId, exerciseStore.currentQuestion.question?.questionId, newViolationLaws);
        if (!questionStore.question) {
            console.log(`no need to generate sup question`);
            // remove redurant feedback
            //if (!!exerciseStore.currentQuestion.feedback?.messages) {
            //    exerciseStore.currentQuestion.feedback.messages = exerciseStore.currentQuestion.feedback.messages.filter(m => m.type !== 'ERROR' || m.violationLaws?.[0] !== violationLaws?.[0]);
            //}
            setAllVisible(false);
        }        
    }

    if (!violationLaws.length) {
        return null;
    }
    return (
        <Optional isVisible={isAllVisible}>
            <Optional isVisible={isButtonsVisible}>
                <div className="d-flex flex-row">
                    <Button onClick={onDetailsClicked} variant="primary">{t('generateSupQuestion_details')}</Button>
                    <Button onClick={onGotitClicked} variant="success" className="ml-2">{t('generateSupQuestion_gotit')}</Button>
                </div>
            </Optional>
            
            
            <Modal type={'DIALOG'}
                size={'xl'}
                show={isModalVisible} 
                closeButton={false} 
                handleClose={() => setIsModalVisible(false)}>
                <Question store={questionStore} showExtendedFeedback={false} onChanged={OnAnswered}/>
            </Modal>
        </Optional>
    )
})
