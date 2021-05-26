import { observer } from 'mobx-react';
import * as React from 'react';
import { useState } from 'react';
import { Button } from 'react-bootstrap';
import { container } from "tsyringe";
import { ExerciseStore } from "../../stores/exercise-store";
import { QuestionStore } from '../../stores/question-store';
import { Modal } from '../common/modal';
import { Optional } from '../common/optional';
import { Question } from './question';

export const GenerateSupQuestion = observer(({ violationLaws } : { violationLaws: string[] }) => {
    const [exerciseStore] = useState(() => container.resolve(ExerciseStore));
    const [questionStore] = useState(() => container.resolve(QuestionStore));
    const [isModalVisible, setIsModalVisible] = useState(false);
    const [isAllVisible, setAllVisible] = useState(true);
    const onClicked = (e: React.MouseEvent<HTMLElement>) => {
        (async () => {
            setIsModalVisible(true);
            if (!exerciseStore.currentAttempt?.attemptId || !violationLaws.length || !exerciseStore.currentQuestion.question) {
                return;
            }
            await questionStore.generateSupplementaryQuestion(exerciseStore.currentAttempt.attemptId, exerciseStore.currentQuestion.question?.questionId, violationLaws);
            if (!questionStore.question) {
                console.log(`no need to generate sup question`);
                setAllVisible(false);
            }
        })();
    }
    const OnAnswered = async () => {
        const newViolationLaws = questionStore.feedback?.messages && questionStore.feedback.messages?.[0].type === 'ERROR' && questionStore.feedback.messages?.[0].violationLaws || [];
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
            <div style={{ marginTop: '20px'}}>            
                <Button onClick={onClicked} variant="primary">Supplementary question</Button>
                <Modal type={'DIALOG'}
                    size={'xl'}
                    show={isModalVisible} 
                    closeButton={false} 
                    handleClose={() => setIsModalVisible(false)}>
                    <Question store={questionStore} showExtendedFeedback={false} onChanged={OnAnswered}/>
                </Modal>
            </div>
        </Optional>
        
    )
})
