import { ExerciseAttempt, TExerciseAttempt, TOptionalExerciseAttemptResult } from "./types/exercise-attempt";
import { ExerciseStatisticsItem, TExerciseStatisticsItems } from "./types/exercise-statistics";
import { Feedback, TFeedback } from "./types/feedback";
import { Interaction } from "./types/interaction";
import { Question, TQuestion } from "./types/question";
import { SessionInfo, TSessionInfo } from "./types/session-info";
import { ajaxGet, ajaxPost, PromiseEither, RequestError } from "./utils/ajax";



export class Api {
    static endpointPath: string = Api.initEndpointPath();
    private static initEndpointPath() {
        const matches = /^\/(?:.+\/)?/.exec(window.location.pathname)
        if (matches) {
            return matches[0];
        }
        return "/";
    }

    static loadSessionInfo(): PromiseEither<RequestError, SessionInfo> {
        return ajaxGet(`${Api.endpointPath}loadSessionInfo`, TSessionInfo);
    }

    static getQuestion(questionId: number): PromiseEither<RequestError, Question> {
        return ajaxGet(`${Api.endpointPath}getQuestion?questionId=${questionId}`, TQuestion);
    }

    static generateQuestion(attemptId: number): PromiseEither<RequestError, Question> {
        return ajaxGet(`${Api.endpointPath}generateQuestion?attemptId=${attemptId}`, TQuestion); 
    }

    static addQuestionAnswer(interaction: Interaction): PromiseEither<RequestError, Feedback> {
        return ajaxPost(`${Api.endpointPath}addQuestionAnswer`, interaction, TFeedback);
    }

    static getExistingExerciseAttempt(exerciseId: number): PromiseEither<RequestError, ExerciseAttempt | null | undefined | ''> {
        return ajaxGet(`${Api.endpointPath}getExistingExerciseAttempt?exerciseId=${exerciseId}`, TOptionalExerciseAttemptResult); 
    }

    static createExerciseAttempt(exerciseId: number): PromiseEither<RequestError, ExerciseAttempt> {
        return ajaxGet(`${Api.endpointPath}createExerciseAttempt?exerciseId=${exerciseId}`, TExerciseAttempt); 
    }

    static getExerciseStatistics(exerciseId: number): PromiseEither<RequestError, ExerciseStatisticsItem[]> {
        return ajaxGet(`${Api.endpointPath}getExerciseStatistics?exerciseId=${exerciseId}`, TExerciseStatisticsItems)
    }
}
