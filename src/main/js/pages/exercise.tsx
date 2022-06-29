import { observer } from "mobx-react";
import React, { useCallback, useEffect, useState } from "react";
import { GenerateNextQuestionBtn } from "../components/exercise/generate-next-question-btn";
import { Header } from "../components/exercise/header";
import { LoadingWrapper } from "../components/common/loader";
import { Modal } from "../components/common/modal";
import { GenerateNextAnswerBtn } from "../components/exercise/generate-next-answer-btn";
import { container } from "tsyringe";
import { ExerciseStore } from "../stores/exercise-store";
import { CurrentQuestion } from "../components/exercise/current-question";
import { Optional } from "../components/common/optional";
import { useTranslation } from "react-i18next";
import { Alert } from "react-bootstrap";
import { SurveyComponent } from "../components/exercise/survey";
import { Survey } from "../types/survey";

export const Exercise = observer(() => {
    const [exerciseStore] = useState(() => container.resolve(ExerciseStore));
    const { exerciseState, setExerciseState, storeState:excerciseStoreState, currentQuestion, surveyResults } = exerciseStore;
    const { storeState:currentQuestionStoreState } = currentQuestion;
    const { t } = useTranslation();

    // on first render
    useEffect(() => {
        (async () => {
            if (exerciseState === 'LAUNCH_ERROR') {
                return;
            }

            if (exerciseStore.currentQuestion.question) {
                setExerciseState('EXERCISE');
                return;
            }

            await exerciseStore.loadSessionInfo();
            if (exerciseStore.sessionInfo?.exercise.options.forceNewAttemptCreationEnabled || 
                !(await exerciseStore.loadExistingExerciseAttempt())) {
                setExerciseState('EXERCISE');
                createAttemptAndLoadQuestion();
            } else {
                setExerciseState('MODAL');
            }
        })()
    }, []);

    const loadQuestion = useCallback(() => {
        (async () => {
            if (exerciseStore.currentAttempt?.questionIds.length) {
                const len = exerciseStore.currentAttempt?.questionIds.length;
                await exerciseStore.currentQuestion.loadQuestion(exerciseStore.currentAttempt?.questionIds[len - 1]);
            } else {
                await exerciseStore.generateQuestion();
            }
        })()
    }, [exerciseStore]);

    const createAttemptAndLoadQuestion = useCallback(() => {
        (async () => {
            exerciseStore.currentQuestion.setQuestionState('LOADING');
            await exerciseStore.createExerciseAttempt();
            loadQuestion();
        })()
    }, [exerciseStore]);

    const onSurveyAnswered = useCallback((survey: Survey, questionId: number, answers: Record<number, string>) => {
        exerciseStore.setSurveyAnswers(questionId, answers);
    }, [exerciseStore]);

    return (
        <>
            <LoadingWrapper isLoading={exerciseStore.isSessionLoading === true || exerciseState === 'INITIAL'}>
                <Optional isVisible={exerciseState === 'EXERCISE' || exerciseState === 'COMPLETED'}>
                    <Header />
                    <div className="mt-5">
                        <CurrentQuestion />
                        <Optional isVisible={exerciseState === 'EXERCISE'}>                            
                            <Optional isVisible={exerciseStore.currentQuestion.questionState === 'COMPLETED' 
                                && exerciseStore.sessionInfo?.exercise.options.surveyOptions?.enabled === true
                                && surveyResults[exerciseStore.currentQuestion.question?.questionId ?? -1] === undefined}>
                                <div className="mt-2">
                                    <SurveyComponent questionId={exerciseStore.currentQuestion.question?.questionId ?? -1} 
                                                     surveyId={exerciseStore.sessionInfo?.exercise.options.surveyOptions?.surveyId ?? ''} 
                                                     onAnswered={onSurveyAnswered}/>
                                </div>
                            </Optional>
                            <Optional isVisible={exerciseStore.currentQuestion.questionState === 'LOADED'}>
                                <div className="mt-3">
                                    <GenerateNextAnswerBtn />
                                </div>
                            </Optional>
                            <Optional isVisible={
                                exerciseStore.currentQuestion.questionState !== 'COMPLETED' || 
                                !exerciseStore.sessionInfo?.exercise.options.surveyOptions?.enabled || 
                                 surveyResults[exerciseStore.currentQuestion.question?.questionId ?? -1] !== undefined}>
                                <div className="mt-2">
                                    <GenerateNextQuestionBtn />
                                </div>
                            </Optional>
                        </Optional>
                        <Optional isVisible={exerciseState === 'COMPLETED'}>
                            <div className="mt-3">
                                <Alert variant={'success'}>
                                    {t('exercise_completed')!}
                                </Alert>
                            </div>
                        </Optional>
                    </div>                
                </Optional>
                <Optional isVisible={exerciseState === 'MODAL'}>
                    <Modal  type={'DIALOG'}
                            title={t('foundExisitingAttempt_title')}
                            primaryBtnTitle={t('foundExisitingAttempt_continueattempt')}
                            handlePrimaryBtnClicked={() => {
                                setExerciseState('EXERCISE');
                                loadQuestion();
                            }}
                            secondaryBtnTitle={t('foundExisitingAttempt_newattempt')}
                            handleSecondaryBtnClicked={() => {
                                setExerciseState('EXERCISE');
                                createAttemptAndLoadQuestion();
                            }}>
                        <p>{t('foundExisitingAttempt_descr')}?</p>
                    </Modal>
                </Optional>
            </LoadingWrapper>
            {
                [excerciseStoreState, currentQuestionStoreState]
                    .filter(x => x.tag === 'ERROR')
                    .map((x, idx, arr) => x.tag === 'ERROR' && <div className="mt-2"><Alert variant='danger'>{x.error.message}</Alert></div>)
            }
        </>
    );
})
