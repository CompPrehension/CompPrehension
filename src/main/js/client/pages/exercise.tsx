import { observer } from "mobx-react";
import React, { useEffect, useState } from "react";
import { Feedback } from "../components/exercise/feedback";
import { GenerateNextQuestionBtn } from "../components/exercise/generate-next-question-btn";
import { Header } from "../components/exercise/header";
import { Loader } from "../components/common/loader";
import { Modal } from "../components/common/modal";
import { QuestionFabric } from "../components/exercise/question/question-fabric";
import { exerciseStore } from "../stores/exercise-store";
import { GenerateNextAnswerBtn } from "../components/exercise/generate-next-answer-btn";

export const Exercise = observer(() => {
    type State = 'INITIAL' | 'MODAL' | 'EXERCISE';
    const [state, setState] = useState('INITIAL' as State);

    // on first render
    useEffect(() => {
        (async () => {
            if (exerciseStore.currentQuestion) {
                setState('EXERCISE');
                return;
            }

            await exerciseStore.loadSessionInfo();
            const attemptExistis = await exerciseStore.loadExistingExerciseAttempt();
            if (attemptExistis) {
                setState('MODAL');
            } else {
                setState('EXERCISE');
                await createAttemptAndLoadQuestion();
            }
        })()
    }, []);

    const loadQuestion = async () => {        
        if (exerciseStore.currentAttempt?.questionIds.length) {
            const len = exerciseStore.currentAttempt?.questionIds.length;
            await exerciseStore.loadQuestion(exerciseStore.currentAttempt?.questionIds[len - 1]);
        } else {
            await exerciseStore.generateQuestion();
        }        
    }

    const createAttemptAndLoadQuestion = async () => {
        exerciseStore.isQuestionLoading = true;
        await exerciseStore.createExerciseAttempt();
        await loadQuestion();
    }

    switch(state) {
        case 'INITIAL':
            return <Loader />;
        case 'MODAL':
            return (
                <Modal title={"Found existing attempt"}
                       primaryBtnTitle="Continue"
                       handlePrimaryBtnClicked={() => {
                            setState('EXERCISE');
                            loadQuestion();
                       }}
                       secondaryBtnTitle="New"
                       handleSecondaryBtnClicked={() => {
                            setState('EXERCISE');
                            createAttemptAndLoadQuestion();
                       }}>
                    <p>Would you like to continue the existing attempt or start a new one?</p>
                </Modal>
            );
        case 'EXERCISE':
            return (
                <>
                    <Header />
                    <QuestionFabric />
                    <Feedback />
                    <GenerateNextAnswerBtn /> 
                    <GenerateNextQuestionBtn />  
                </>
            );
    }
})
