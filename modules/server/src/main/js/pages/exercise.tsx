import { observer } from "mobx-react";
import React, { useCallback, useEffect, useState } from "react";
import { Alert } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import { container } from "tsyringe";
import { LoadingWrapper } from "../components/common/loader";
import { Modal } from "../components/common/modal";
import { Optional } from "../components/common/optional";
import { CurrentQuestion } from "../components/exercise/current-question";
import { ExerciseGenerateNextAnswerBtn } from "../components/exercise/exercise-generate-next-answer-btn";
import { GenerateNextQuestionBtn } from "../components/exercise/generate-next-question-btn";
import { ExerciseHeader } from "../components/exercise/header";
import { SurveyComponent } from "../components/exercise/survey";
import { ExerciseStore } from "../stores/exercise-store";
import { Survey } from "../types/survey";

export const Exercise = observer(() => {
    const [exerciseStore] = useState(() => container.resolve(ExerciseStore));
    const { exerciseState, setExerciseState, storeState:excerciseStoreState, currentQuestion, survey } = exerciseStore;
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
            
            if (exerciseStore.isDebug) {
                setExerciseState('EXERCISE');
                createDebugAttemptAndLoadQuestion();
                return;
            }

            const attemptId = exerciseStore.currentAttemptId;
            if (attemptId && Number.isInteger(attemptId)) {
                setExerciseState('EXERCISE');
                getAttemptAndLoadQuestion(+attemptId);
                return;
            }

            if (exerciseStore.exercise?.options.forceNewAttemptCreationEnabled || 
                !(await exerciseStore.loadExistingExerciseAttempt())) {
                setExerciseState('EXERCISE');
                createAttemptAndLoadQuestion();
                return;
            }
                
            setExerciseState('MODAL');
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
    const createDebugAttemptAndLoadQuestion = useCallback(() => {
        (async () => {
            exerciseStore.currentQuestion.setQuestionState('LOADING');
            await exerciseStore.createDebugExerciseAttempt();
            loadQuestion();
        })()
    }, [exerciseStore]);
    const getAttemptAndLoadQuestion = useCallback((attemptId: number) => {
        (async () => {
            exerciseStore.currentQuestion.setQuestionState('LOADING');
            await exerciseStore.loadExerciseAttempt(attemptId);
            loadQuestion();
        })()
    }, [exerciseStore]);

    const onSurveyAnswered = useCallback((survey: Survey, questionId: number, answers: Record<number, string>) => {
        exerciseStore.setSurveyAnswers(questionId, answers);
    }, [exerciseStore]);

    return (
        <>
            <div className={`compph-exercise ${exerciseStore.isDebug && 'compph-exercise--debug'}` || ''}>
                <LoadingWrapper isLoading={exerciseStore.isSessionLoading === true || exerciseState === 'INITIAL'}>
                    <Optional isVisible={exerciseState === 'EXERCISE' || exerciseState === 'COMPLETED'}>
                        <ExerciseHeader />
                        <div className="mt-5">
                            <CurrentQuestion />
                            {survey != null 
                                && (exerciseStore.currentQuestion.questionState === 'COMPLETED' || exerciseState === 'COMPLETED') 
                                &&  <div className="mt-2">
                                        <SurveyComponent questionId={exerciseStore.currentQuestion.question?.questionId ?? -1} 
                                                        survey={survey!.survey}
                                                        enabledSurveyQuestions={exerciseStore.ensureQuestionSurveyExists(currentQuestion.question?.questionId ?? -1)}
                                                        value={survey!.questions[exerciseStore.currentQuestion.question?.questionId ?? -1]?.results}
                                                        onAnswersSended={onSurveyAnswered}
                                                        isCompleted={survey.questions[exerciseStore.currentQuestion.question?.questionId ?? -1]?.status === 'COMPLETED'}/>                                                     
                                    </div>}
                            <Optional isVisible={exerciseState === 'EXERCISE'}>
                                <Optional isVisible={exerciseStore.currentQuestion.questionState === 'LOADED'}>
                                    <div className="mt-3">
                                        <ExerciseGenerateNextAnswerBtn />
                                    </div>
                                </Optional>
                                <Optional isVisible={
                                    (survey == null && (exerciseStore.exercise?.options.newQuestionGenerationEnabled || exerciseStore.currentQuestion.questionState === 'COMPLETED'))
                                    || 
                                    ((survey != null && survey.questions[exerciseStore.currentQuestion.question?.questionId ?? -1]?.status === 'COMPLETED'))}>
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

            </div>
            
        </>
    );
})
