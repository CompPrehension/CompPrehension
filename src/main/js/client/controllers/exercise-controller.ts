import { ExerciseAttempt, TExerciseAttempt, TOptionalExerciseAttemptResult } from "../types/exercise-attempt";
import { ExerciseStatisticsItem, TExerciseStatisticsItems } from "../types/exercise-statistics";
import { Feedback, TFeedback } from "../types/feedback";
import { Interaction } from "../types/interaction";
import { Question, TQuestion } from "../types/question";
import { SessionInfo, TSessionInfo } from "../types/session-info";
import { ajaxGet, ajaxPost, PromiseEither, RequestError } from "../utils/ajax";



export class ExerciseController {
    static endpointPath: string = ExerciseController.initEndpointPath();
    private static initEndpointPath() {
        const matches = /^\/(?:.+\/(?<!\/pages\/))?/.exec(window.location.pathname)
        if (matches) {
            return matches[0];
        }
        return "/";
    }

    static loadSessionInfo(): PromiseEither<RequestError, SessionInfo> {
        return ajaxGet(`${ExerciseController.endpointPath}loadSessionInfo`, TSessionInfo);
    }

    static getQuestion(questionId: number): PromiseEither<RequestError, Question> {
        return ajaxGet(`${ExerciseController.endpointPath}getQuestion?questionId=${questionId}`, TQuestion);
    }

    static generateQuestion(attemptId: number): PromiseEither<RequestError, Question> {
        return ajaxGet(`${ExerciseController.endpointPath}generateQuestion?attemptId=${attemptId}`, TQuestion); 
    }

    static generateNextCorrectAnswer(questionId: number): PromiseEither<RequestError, Feedback> {
        return ajaxGet(`${ExerciseController.endpointPath}generateNextCorrectAnswer?questionId=${questionId}`, TFeedback);
    }

    static addQuestionAnswer(interaction: Interaction): PromiseEither<RequestError, Feedback> {
        return ajaxPost(`${ExerciseController.endpointPath}addQuestionAnswer`, interaction, TFeedback);
    }

    static getExistingExerciseAttempt(exerciseId: number): PromiseEither<RequestError, ExerciseAttempt | null | undefined | ''> {
        return ajaxGet(`${ExerciseController.endpointPath}getExistingExerciseAttempt?exerciseId=${exerciseId}`, TOptionalExerciseAttemptResult); 
    }

    static createExerciseAttempt(exerciseId: number): PromiseEither<RequestError, ExerciseAttempt> {
        return ajaxGet(`${ExerciseController.endpointPath}createExerciseAttempt?exerciseId=${exerciseId}`, TExerciseAttempt); 
    }

    static getExerciseStatistics(exerciseId: number): PromiseEither<RequestError, ExerciseStatisticsItem[]> {
        return ajaxGet(`${ExerciseController.endpointPath}getExerciseStatistics?exerciseId=${exerciseId}`, TExerciseStatisticsItems)
    }
}
