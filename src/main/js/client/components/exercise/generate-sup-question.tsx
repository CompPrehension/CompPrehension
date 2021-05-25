import { observer } from 'mobx-react';
import * as React from 'react';
import { useState } from 'react';
import { Button } from 'react-bootstrap';
import { container } from "tsyringe";
import { ExerciseStore } from "../../stores/exercise-store";
import { QuestionStore } from '../../stores/question-store';
import { Modal } from '../common/modal';
import { Question } from './question';

export const GenerateSupQuestion = observer(({ violations } : { violations: number[] }) => {
    const [exerciseStore] = useState(() => container.resolve(ExerciseStore));
    const [questionStore] = useState(() => container.resolve(QuestionStore));
    const [isModalVisible, setIsModalVisible] = useState(false);
    const onClicked = (e: React.MouseEvent<HTMLElement>) => {
        (async () => {
            setIsModalVisible(true);
            if (!exerciseStore.currentAttempt?.attemptId || !violations.length) {
                return;
            }
            await questionStore.generateSupplementaryQuestion(exerciseStore.currentAttempt.attemptId, violations);
        })();
    }

    if (!violations.length) {
        return null;
    }
    return (
        <div style={{ marginTop: '20px'}}>            
            <Button onClick={onClicked} variant="primary">Supplementary question</Button>
            <Modal type={'DIALOG'}
                   size={'xl'}
                   show={isModalVisible} 
                   closeButton={false} 
                   handleClose={() => setIsModalVisible(false)}>
                <Question store={questionStore} showExtendedFeedback={false} />
            </Modal>
        </div>
    )
})
