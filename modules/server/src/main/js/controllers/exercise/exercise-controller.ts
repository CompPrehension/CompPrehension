import { injectable } from "tsyringe";
import { ExerciseAttempt, TExerciseAttempt, TOptionalExerciseAttemptResult } from "../../types/exercise-attempt";
import { ExerciseStatisticsItem, TExerciseStatisticsItems } from "../../types/exercise-statistics";
import { Feedback, TFeedback } from "../../types/feedback";
import { Interaction } from "../../types/interaction";
import { Question, TOptionalQuestion, TQuestion } from "../../types/question";
import { SessionInfo, TSessionInfo } from "../../types/session-info";
import { SupplementaryQuestionRequest } from "../../types/supplementary-question-request";
import { ajaxGet, ajaxPost, PromiseEither } from "../../utils/ajax";
import * as io from 'io-ts'
import { RequestError } from "../../types/request-error";
import { API_URL } from "../../appconfig";


export interface IExerciseController {
    loadSessionInfo(): PromiseEither<RequestError, SessionInfo>;
    getQuestion(questionId: number): PromiseEither<RequestError, Question>;
    generateQuestion(attemptId: number): PromiseEither<RequestError, Question>;
    generateSupplementaryQuestion(questionRequest: SupplementaryQuestionRequest): PromiseEither<RequestError, Question | null | undefined | ''>;
    generateNextCorrectAnswer(questionId: number): PromiseEither<RequestError, Feedback>;
    addQuestionAnswer(interaction: Interaction): PromiseEither<RequestError, Feedback> ;
    getExistingExerciseAttempt(exerciseId: number): PromiseEither<RequestError, ExerciseAttempt | null | undefined | ''>;
    getExerciseAttempt(attemptId: number): PromiseEither<RequestError, ExerciseAttempt>;
    createDebugExerciseAttempt(exerciseId: number): PromiseEither<RequestError, ExerciseAttempt>;
    createExerciseAttempt(exerciseId: number): PromiseEither<RequestError, ExerciseAttempt>;
    getExerciseStatistics(exerciseId: number): PromiseEither<RequestError, ExerciseStatisticsItem[]>;
    getExercises(): PromiseEither<RequestError, number[]>
}

@injectable()
export class ExerciseController implements IExerciseController {
        static endpointPath: string = ExerciseController.initEndpointPath();
    private static initEndpointPath(): string {
        const matches = /^\/(?:.+\/(?<!\/pages\/))?/.exec(window.location.pathname)
        if (matches) {
            return `${API_URL}${matches[0].substring(1)}`;
        }
        return API_URL;
    }

    loadSessionInfo(): PromiseEither<RequestError, SessionInfo> {
        return ajaxGet(`${ExerciseController.endpointPath}loadSessionInfo`, TSessionInfo);
    }

    getQuestion(questionId: number): PromiseEither<RequestError, Question> {
        return ajaxGet(`${ExerciseController.endpointPath}getQuestion?questionId=${questionId}`, TQuestion);
    }

    generateQuestion(attemptId: number): PromiseEither<RequestError, Question> {
        return ajaxGet(`${ExerciseController.endpointPath}generateQuestion?attemptId=${attemptId}`, TQuestion); 
    }

    generateSupplementaryQuestion(questionRequest: SupplementaryQuestionRequest): PromiseEither<RequestError, Question | null | undefined | ''> {
        return ajaxPost(`${ExerciseController.endpointPath}generateSupplementaryQuestion`, questionRequest, TOptionalQuestion);
    }

    generateNextCorrectAnswer(questionId: number): PromiseEither<RequestError, Feedback> {
        return ajaxGet(`${ExerciseController.endpointPath}generateNextCorrectAnswer?questionId=${questionId}`, TFeedback);
    }

    addQuestionAnswer(interaction: Interaction): PromiseEither<RequestError, Feedback> {
        return ajaxPost(`${ExerciseController.endpointPath}addQuestionAnswer`, interaction, TFeedback);
    }

    getExerciseAttempt(attemptId: number): PromiseEither<RequestError, ExerciseAttempt> {
        return ajaxGet(`${ExerciseController.endpointPath}getExerciseAttempt?attemptId=${attemptId}`, TExerciseAttempt);
    }

    getExistingExerciseAttempt(exerciseId: number): PromiseEither<RequestError, ExerciseAttempt | null | undefined | ''> {
        return ajaxGet(`${ExerciseController.endpointPath}getExistingExerciseAttempt?exerciseId=${exerciseId}`, TOptionalExerciseAttemptResult);
    }

    createExerciseAttempt(exerciseId: number): PromiseEither<RequestError, ExerciseAttempt> {
        return ajaxGet(`${ExerciseController.endpointPath}createExerciseAttempt?exerciseId=${exerciseId}`, TExerciseAttempt); 
    }

    createDebugExerciseAttempt(exerciseId: number): PromiseEither<RequestError, ExerciseAttempt> {
        return ajaxGet(`${ExerciseController.endpointPath}createDebugExerciseAttempt?exerciseId=${exerciseId}`, TExerciseAttempt); 
    }

    getExerciseStatistics(exerciseId: number): PromiseEither<RequestError, ExerciseStatisticsItem[]> {
        return ajaxGet(`${ExerciseController.endpointPath}getExerciseStatistics?exerciseId=${exerciseId}`, TExerciseStatisticsItems)
    }

    getExercises(): PromiseEither<RequestError, number[]> {
        return ajaxGet(`${ExerciseController.endpointPath}getExercises`, io.array(io.number));
    }
}
