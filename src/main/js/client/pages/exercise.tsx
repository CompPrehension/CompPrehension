import { observer } from "mobx-react";
import React, { useEffect, useState } from "react";
import { Feedback } from "../components/feedback";
import { GenerateNextQuestionBtn } from "../components/generate-next-question-btn";
import { Header } from "../components/header";
import { Loader } from "../components/loader";
import { Modal } from "../components/modal";
import { QuestionFabric } from "../components/question/question-fabric";
import { exerciseStore } from "../stores/exercise-store";

export const Exercise = observer(() => {
    type State = 'INITIAL' | 'MODAL' | 'EXERCISE';
    const [state, setState] = useState('INITIAL' as State);

    // on first render
    useEffect(() => {
        (async () => {
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
                    <GenerateNextQuestionBtn />            
                </>
            );
    }
})
