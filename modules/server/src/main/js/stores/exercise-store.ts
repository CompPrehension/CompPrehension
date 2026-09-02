import * as E from "fp-ts/lib/Either";
import { action, autorun, makeAutoObservable, toJS } from "mobx";
import { exerciseController, surveyController } from "../controllers";
import { Exercise } from "../types/exercise";
import { ExerciseAttempt } from "../types/exercise-attempt";
import { RequestError } from "../types/request-error";
import { Survey, SurveyQuestion } from "../types/survey";
import { getUrlParameterByName } from "../types/utils";
import { QuestionStore } from "./question-store";

export class ExerciseStore {
    isExerciseLoading: boolean = false;
    exerciseId: number;
    courseId?: number = undefined;
    exercise?: Exercise = undefined;
    currentAttemptId?: number = undefined;
    currentAttempt?: ExerciseAttempt = undefined;
    currentQuestion: QuestionStore;
    exerciseState: 'LAUNCH_ERROR' | 'INITIAL' | 'MODAL' | 'EXERCISE' | 'COMPLETED' = 'INITIAL';
    storeState: { tag: 'VALID' } | { tag: 'ERROR', error: RequestError, } = { tag: 'VALID' };
    survey?: ExerciseSurveySettings = undefined;
    isDebug = false;

    constructor() {
        // calc store initial state
        this.isDebug = getUrlParameterByName('debug') !== null;
        this.currentQuestion = new QuestionStore();

        const rawExerciseId = getUrlParameterByName('exerciseId');
        if (rawExerciseId === null) {
            this.exerciseState = 'LAUNCH_ERROR';
            this.storeState = { tag: 'ERROR', error: { message: "Invalid exercise id" } };
        }
        this.exerciseId = rawExerciseId !== null ? +rawExerciseId : -1;

        const rawCourseId = getUrlParameterByName('courseId');
        if (rawCourseId !== null) {
            this.courseId = +rawCourseId;
        }

        const rawAttemptId = getUrlParameterByName('attemptId');
        if (rawAttemptId !== null) {
            this.currentAttemptId = +rawAttemptId;
        }

        // both of these are called from inside a derivation - setExerciseState from the
        // autorun below, ensureQuestionSurveyExists straight from the exercise page render -
        // and they write state. `makeAutoObservable` would infer `autoAction`, which keeps
        // tracking in that situation and would make each call invalidate its own caller;
        // a real action untracks, which is what they need.
        makeAutoObservable(this, {
            setExerciseState: action,
            ensureQuestionSurveyExists: action,
        });
        this.registerOnStrategyDecisionChangedAction();
    }

    private registerOnStrategyDecisionChangedAction = () => {
        autorun(() => {
            if (this.currentQuestion.feedback?.strategyDecision === 'FINISH' && this.exerciseState !== 'COMPLETED') {
                this.setExerciseState('COMPLETED');
            }
        })
    }

    private forceSetValidState = () => {
        if (this.storeState.tag !== 'VALID') {
            this.storeState = { tag: 'VALID' };
        }
    }

    setExerciseState = (newState: ExerciseStore['exerciseState']) => {
        if (this.exerciseState !== newState) {
            this.exerciseState = newState;
        }
    }

    setSurveyAnswers = (quesionId: number, answers: Record<number, string>) => {
        if (!this.survey)
            return;

        this.survey.questions[quesionId].status = 'COMPLETED';
        this.survey.questions[quesionId].results = answers;
    }

    loadExercise = async () => {
        if (this.exercise) {
            throw new Error("exerciseInfo loaded");
        }
        if (this.isExerciseLoading) {
            return;
        }

        this.forceSetValidState();
        this.isExerciseLoading = true;

        const exercise = await exerciseController.getExerciseShortInfo(this.exerciseId, this.courseId);
        this.isExerciseLoading = false;

        if (E.isRight(exercise)) {
            this.exercise = exercise.right;
        } else {
            this.storeState = { tag: 'ERROR', error: exercise.left };
        }
    };

    loadExerciseAttempt = async (attemptId: number) => {
        if (!this.exercise) {
            throw new Error("exerciseInfo is not defined");
        }

        this.forceSetValidState();
        const resultEither = await exerciseController.getExerciseAttempt(attemptId);
        if (E.isLeft(resultEither)) {
            this.storeState = { tag: 'ERROR', error: resultEither.left };
            return;
        }

        if (!resultEither.right) {
            return false;
        }

        this.currentAttempt = resultEither.right;
        await this.onAttemptLoaded();
        return true;
    };

    loadExistingExerciseAttempt = async () => {
        const { exercise } = this;
        if (!exercise) {
            throw new Error("exercise is not defined");
        }

        this.forceSetValidState();
        const resultEither = await exerciseController.getExistingExerciseAttempt(exercise.id, this.courseId);
        if (E.isLeft(resultEither)) {
            this.storeState = { tag: 'ERROR', error: resultEither.left };
            return;
        }

        if (!resultEither.right) {
            return false;
        }

        this.currentAttempt = resultEither.right;
        await this.onAttemptLoaded();
        return true;
    };

    onAttemptLoaded = async () => {
        await this.loadSurvey();
    }

    createExerciseAttempt = async () => {
        const { exercise } = this;
        if (!exercise) {
            throw new Error("exercise is not defined");
        }

        this.forceSetValidState();
        const resultEither = await exerciseController.createExerciseAttempt(exercise.id, this.courseId);
        if (E.isLeft(resultEither)) {
            this.storeState = { tag: 'ERROR', error: resultEither.left };
            return;
        }

        this.currentAttempt = resultEither.right;
        await this.onAttemptLoaded();
    };

    createDebugExerciseAttempt = async () => {
        const { exercise } = this;
        if (!exercise) {
            throw new Error("exercise is not defined");
        }

        this.forceSetValidState();
        const resultEither = await exerciseController.createDebugExerciseAttempt(exercise.id, this.courseId);
        if (E.isLeft(resultEither)) {
            this.storeState = { tag: 'ERROR', error: resultEither.left };
            return;
        }

        this.currentAttempt = resultEither.right;
        await this.onAttemptLoaded();
    };

    generateQuestion = async () => {
        const { exercise, currentAttempt } = this;
        if (!exercise || !currentAttempt) {
            throw new Error("Session is not defined");
        }

        this.forceSetValidState();
        await this.currentQuestion.generateQuestion(currentAttempt.attemptId);
        currentAttempt.questionIds.push(this.currentQuestion.question?.questionId ?? -1);
    };

    loadSurvey = async () => {
        if (this.survey || !this.currentAttempt || !this.exercise)
            return;
        if (!this.exercise.options.surveyOptions?.enabled || this.exercise.options.surveyOptions.surveyId.length === 0)
            return;

        const surveyId = this.exercise.options.surveyOptions.surveyId;
        const attemptId = this.currentAttempt.attemptId;
        const [survey, surveyResults] = await Promise.all([
            surveyController.getSurvey(surveyId),
            surveyController.getCurrentUserAttemptSurveyVotes(surveyId, attemptId),
        ]);

        if (E.isRight(survey) && E.isRight(surveyResults)) {
            const tmp = groupBy(surveyResults.right, x => x.questionId)
            this.survey = {
                survey: survey.right,
                questions: [...tmp.keys()].map(k => ({
                    questionId: k,
                    status: 'COMPLETED' as const,
                    questions: tmp.get(k)?.map(z => z.surveyQuestionId) ?? [],
                    results: tmp.get(k)?.reduce((acc, z) => (acc[z.surveyQuestionId] = z.answer, acc), {} as Record<number, string>) ?? {},
                })).reduce((acc, i) => (acc[i.questionId] = i, acc), {} as Record<number, QuestionSurveyResult>),
            }
        }
    }

    ensureQuestionSurveyExists = (questionId: number) => {
        if (this.survey?.questions[questionId])
            return this.survey?.questions[questionId].questions;

        const qs: SurveyQuestion[] = [];
        const currentQuestionIdx = this.currentAttempt!.questionIds.findIndex(z => z === this.currentQuestion.question?.questionId)
        for (let q of this.survey?.survey.questions || []) {
            const policy = q.policy;
            if (policy.kind === 'AFTER_EACH'
                || policy.kind === 'AFTER_FIRST' && currentQuestionIdx === 0
                || policy.kind === 'AFTER_LAST' && this.exerciseState === 'COMPLETED'
                || policy.kind === 'AFTER_SPECIFIC' && policy.numbers.includes(currentQuestionIdx + 1)) {
                qs.push(q);
            }
        }
        console.log("Selected questions")
        console.log(toJS(qs))

        var questionSurvey: QuestionSurveyResult = {
            questionId: questionId,
            status: 'ACTIVE',
            questions: qs.map(z => z.id),
            results: {},
        };

        this.survey!.questions[questionId] = questionSurvey;
        return qs.map(z => z.id);
    }
}

function groupBy<T, K>(list: T[], keyGetter: (z: T) => K) {
    const map = new Map<K, T[]>();
    list.forEach((item) => {
        const key = keyGetter(item);
        const collection = map.get(key);
        if (!collection) {
            map.set(key, [item]);
        } else {
            collection.push(item);
        }
    });
    return map;
}

type ExerciseSurveySettings = {
    survey: Survey,
    questions: Record<number, QuestionSurveyResult>,
}

type QuestionSurveyResult = {
    questionId: number,
    status: 'ACTIVE' | 'COMPLETED',
    questions: number[],
    results: Record<number, string>,
}

let sharedExerciseStore: ExerciseStore | undefined;

/** The exercise page and every component inside it work with one and the same store. */
export function getExerciseStore(): ExerciseStore {
    return sharedExerciseStore ??= new ExerciseStore();
}
