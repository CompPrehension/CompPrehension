import { observer } from "mobx-react";
import React, { useEffect, useState } from "react";
import { GenerateNextQuestionBtn } from "../components/exercise/generate-next-question-btn";
import { Header } from "../components/exercise/header";
import { LoadingWrapper } from "../components/common/loader";
import { Modal } from "../components/common/modal";
import { GenerateNextAnswerBtn } from "../components/exercise/generate-next-answer-btn";
import { container } from "tsyringe";
import { ExerciseStore } from "../stores/exercise-store";
import { CurrentQuestion } from "../components/exercise/current-question";
import { Optional } from "../components/common/optional";

export const Exercise = observer(() => {
    const [exerciseStore] = useState(() => container.resolve(ExerciseStore));
    type State = 'INITIAL' | 'MODAL' | 'EXERCISE';
    const [state, setState] = useState<State>('INITIAL');

    // on first render
    useEffect(() => {
        (async () => {
            if (exerciseStore.currentQuestion.question) {
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
            await exerciseStore.currentQuestion.loadQuestion(exerciseStore.currentAttempt?.questionIds[len - 1]);
        } else {
            await exerciseStore.generateQuestion();
        }        
    }

    const createAttemptAndLoadQuestion = async () => {
        exerciseStore.currentQuestion.isQuestionLoading = true;
        await exerciseStore.createExerciseAttempt();
        await loadQuestion();
    }

    return (
        <LoadingWrapper isLoading={state === 'INITIAL'}>
            <Optional isVisible={state === 'EXERCISE'}>
                <Header />
                <CurrentQuestion />
                <GenerateNextAnswerBtn /> 
                <GenerateNextQuestionBtn />
            </Optional>
            <Optional isVisible={state === 'MODAL'}>
                <Modal  type={'DIALOG'}
                        title={"Found existing attempt"}
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
            </Optional>
        </LoadingWrapper>
    );
})
