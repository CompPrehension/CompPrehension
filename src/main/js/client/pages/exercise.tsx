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
    const { isQuestionLoading } = exerciseStore;

    // on first render
    useEffect(() => {
        (async () => {
            await exerciseStore.session.loadSessionInfo();
            const existingAttempt = await exerciseStore.loadExistingExerciseAttempt();
            setState(existingAttempt ? 'MODAL' : 'EXERCISE');            
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
                            exerciseStore.isQuestionLoading = true;
                            exerciseStore.createExerciseAttempt().then(() => loadQuestion())
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
