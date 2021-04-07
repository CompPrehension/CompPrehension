import { ExerciseStatisticsItem, TExerciseStatisticsItems } from "./types/exercise-statistics";
import { Feedback, TFeedback } from "./types/feedback";
import { Interaction } from "./types/interaction";
import { Question, TQuestion } from "./types/question";
import { SessionInfo, TSessionInfo } from "./types/session-info";
import { ajaxGet, ajaxPost, PromiseEither, RequestError } from "./utils/ajax";



export class Api {
    static endpointPath: string = Api.initEndpointPath();
    private static initEndpointPath() {
        const matches = /^\/(?:[^\/]+\/)?/.exec(window.location.pathname)
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

    static addAnswer(interaction: Interaction): PromiseEither<RequestError, Feedback> {
        return ajaxPost(`${Api.endpointPath}addAnswer`, interaction, TFeedback);
    }

    static getExerciseStatistics(exerciseId: number): PromiseEither<RequestError, ExerciseStatisticsItem[]> {
        return ajaxGet(`${Api.endpointPath}getExerciseStatistics?exerciseId=${exerciseId}`, TExerciseStatisticsItems)
    }
}
