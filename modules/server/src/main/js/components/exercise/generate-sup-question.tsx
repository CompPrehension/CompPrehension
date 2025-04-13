import { observer } from 'mobx-react';
import * as React from 'react';
import {  useState } from 'react';
import { Alert, Button } from 'react-bootstrap';
import { delayPromise } from '../../utils/helpers';
import { Modal } from '../common/modal';
import { Optional } from '../common/optional';
import { useTranslation } from "react-i18next";
import { FeedbackMessage, FeedbackViolationLaw } from '../../types/feedback';
import { SupplementaryQuestionStore } from '../../stores/sup-question-store';
import { Loader } from '../common/loader';
import { Answer } from '../../types/answer';
import { QuestionComponent } from '../common/question/question';
import { toJS } from 'mobx';

type GenerateSupQuestionProps = {
    store: SupplementaryQuestionStore,
    violationLaw: FeedbackViolationLaw[],
}

export const GenerateSupQuestion = observer((props : GenerateSupQuestionProps) => {
    const { violationLaw, store } = props;    
    const [isModalVisible, setIsModalVisible] = useState(false);
    const [isButtonsVisible, setIsButtonsVisible] = useState(true);
    const [isAllVisible, setAllVisible] = useState(true);
    const [currentViolationLaw, setCurrentViolationLaw] = useState(violationLaw);
    const { t } = useTranslation();

    const onDetailsClicked = async () => { 
        setIsButtonsVisible(false);
        setIsModalVisible(true);
        await store.generateSupplementaryQuestion(currentViolationLaw.map(v => v.name));
        if (!store.question || store.feedback?.action === 'FINISH') {
            console.log(`no need to generate sup question`);
            setAllVisible(false);
        }
    }
    const onGotitClicked = () => {
        setAllVisible(false);
    }
    const tryContinueAuto = async () => {
        if ((store.questionState == "COMPLETED" || !store.question) && store.feedback?.action === 'CONTINUE_AUTO') {
            console.log(`show feedback for 3 seconds`);
            await delayPromise(3000);
            await onNextQuestionClicked();
        }
    }
    const onAnswered = async () => {
        await store.sendAnswers();
        const { feedback } = store;
        if (!feedback) {
            console.log(`empty feedback for question asnwer`);
            setAllVisible(false);
            return;
        }
        await tryContinueAuto();
    }
    const onNextQuestionClicked = async () => {
        const newViolationLaw = store.feedback?.message?.violationLaws || null;
        if (!newViolationLaw) {
            console.log(`empty violation laws`);
            setAllVisible(false);
            return;            
        }

        setCurrentViolationLaw(newViolationLaw)
        await store.generateSupplementaryQuestion(newViolationLaw.map(v => v.name));
        await tryContinueAuto();
    }

    return (
        <Optional isVisible={isAllVisible}>
            <Optional isVisible={isButtonsVisible}>
                <div className="d-flex flex-row mt-3">
                    <Button onClick={onDetailsClicked} variant="primary">{t('exercise_supquestion_details')}</Button>
                    <Button onClick={onGotitClicked} variant="success" className="ml-2">{t('exercise_supquestion_gotit')}</Button>
                </div>
            </Optional>            
            <Modal  type={'DIALOG'} size={'xl'} 
                    show={isModalVisible}
                    closeButton={false} 
                    handleClose={() => setIsModalVisible(false)}>
                <SupQuestion store={store} onSubmitted={onAnswered} onNextQuestionRequested={onNextQuestionClicked}/>
            </Modal>
        </Optional>
    )
})

type SupQuestionProps = {
    store: SupplementaryQuestionStore,
    onSubmitted?: () => void,
    onNextQuestionRequested?: () => void,
}
const SupQuestion = observer((props: SupQuestionProps) => {
    const { store, onSubmitted, onNextQuestionRequested } = props;
    const { t } = useTranslation();
    const questionData = store.question;
    if (store.questionState === 'LOADING') {
        return <div className="mt-2"><Loader /></div>;
    }
    
    const onChanged = (newHistory: Answer[]) => {
        store.setAnswer(newHistory);

        if (store.questionSubmitMode === 'IMPLICIT') {
            onSubmitted?.();
        }
    };
    const getAnswer = () => store.answer as Answer[];
    const getFeedback = () => undefined;

    const showSendAnswerButton = store.questionSubmitMode === 'EXPLICIT' && store.canSendQuestionAnswers;
    const showQuestionFeedback = store.questionState === 'COMPLETED' && !!store.feedback && !!questionData;
    const showMessageFeedback = store.questionState === 'COMPLETED' && !!store.feedback && !questionData;
    const showNextQBtn = store.feedback?.action === 'CONTINUE_MANUAL' && (showQuestionFeedback || showMessageFeedback) && !!store.feedback?.message.violationLaws;

    return (
        <>
            {questionData &&
                <QuestionComponent 
                    question={questionData} 
                    answers={store.answer as Answer[]} 
                    getAnswers={getAnswer} 
                    onChanged={onChanged} 
                    getFeedback={getFeedback} 
                    isFeedbackLoading={store.isFeedbackLoading} 
                    isQuestionFreezed={store.isQuestionFreezed}/>
            }
            {store.isFeedbackLoading && 
                <div className="mt-2"><Loader /></div>
            }
            {showSendAnswerButton &&
                <Button variant="primary" onClick={onSubmitted}>{t('exercise_supquestion_send_answer')}</Button>
            }
            {showMessageFeedback &&
                <>{store.feedback!.message.message}</>
            }
            {showQuestionFeedback &&
                <div className='mt-2'>
                    <ShortFeedbackAlert message={store.feedback!.message}/>
                </div>
            }
            {showNextQBtn &&
                <div className='mt-3'>
                    <Button variant="primary" onClick={onNextQuestionRequested}>{t('exercise_supquestion_next_question')}</Button>
                </div>
            }
        </>
    );
})


type ShortFeedbackAlertProps = {    
    message: FeedbackMessage,
    showGenerateSupQuestion?: boolean
    supQuestionStore?: SupplementaryQuestionStore,
}
export const ShortFeedbackAlert = observer((props: ShortFeedbackAlertProps) => {
    let { supQuestionStore, message, showGenerateSupQuestion } = props;    
    showGenerateSupQuestion = showGenerateSupQuestion && supQuestionStore != undefined;

    const variant = message.type === 'SUCCESS' ? 'success' : 'danger';
    return(
        <Alert variant={variant}>
            {message.message}
        </Alert>
    )
})
