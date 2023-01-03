import { injectable } from "tsyringe";
import { ExerciseAttempt, TExerciseAttempt, TOptionalExerciseAttemptResult } from "../../types/exercise-attempt";
import { ExerciseStatisticsItem, TExerciseStatisticsItems } from "../../types/exercise-statistics";
import { Feedback, TFeedback } from "../../types/feedback";
import { Interaction } from "../../types/interaction";
import { Question, TOptionalQuestion, TQuestion } from "../../types/question";
import { SupplementaryQuestionRequest } from "../../types/supplementary-question-request";
import { ajaxGet, ajaxPost, PromiseEither } from "../../utils/ajax";
import * as io from 'io-ts'
import { RequestError } from "../../types/request-error";
import { API_URL } from "../../appconfig";
import { TUserInfo, UserInfo } from "../../types/user-info";
import { Exercise, TExercise } from "../../types/exercise";


export interface IExerciseController {
    // loadSessionInfo(): PromiseEither<RequestError, SessionInfo>;
    getCurrentUser(): PromiseEither<RequestError, UserInfo>;
    getExerciseShortInfo(id: number): PromiseEither<RequestError, Exercise>;
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

    /*
    loadSessionInfo(): PromiseEither<RequestError, SessionInfo> {
        return ajaxGet(`/api/exercise/loadSessionInfo`, TSessionInfo);
    }
    */

    getCurrentUser(): PromiseEither<RequestError, UserInfo> {
        return ajaxGet(`${API_URL}/api/users/whoami`, TUserInfo);
    }

    getExerciseShortInfo(id: number): PromiseEither<RequestError, Exercise> {
        return ajaxGet(`${API_URL}/api/exercise/shortInfo?id=${id}`, TExercise);
    }

    getQuestion(questionId: number): PromiseEither<RequestError, Question> {
        return ajaxGet(`${API_URL}/api/exercise/getQuestion?questionId=${questionId}`, TQuestion);
    }

    generateQuestion(attemptId: number): PromiseEither<RequestError, Question> {
        return ajaxGet(`${API_URL}/api/exercise/generateQuestion?attemptId=${attemptId}`, TQuestion); 
    }

    generateSupplementaryQuestion(questionRequest: SupplementaryQuestionRequest): PromiseEither<RequestError, Question | null | undefined | ''> {
        return ajaxPost(`${API_URL}/api/exercise/generateSupplementaryQuestion`, questionRequest, TOptionalQuestion);
    }

    generateNextCorrectAnswer(questionId: number): PromiseEither<RequestError, Feedback> {
        return ajaxGet(`${API_URL}/api/exercise/generateNextCorrectAnswer?questionId=${questionId}`, TFeedback);
    }

    addQuestionAnswer(interaction: Interaction): PromiseEither<RequestError, Feedback> {
        return ajaxPost(`${API_URL}/api/exercise/addQuestionAnswer`, interaction, TFeedback);
    }

    getExerciseAttempt(attemptId: number): PromiseEither<RequestError, ExerciseAttempt> {
        return ajaxGet(`${API_URL}/api/exercise/getExerciseAttempt?attemptId=${attemptId}`, TExerciseAttempt);
    }

    getExistingExerciseAttempt(exerciseId: number): PromiseEither<RequestError, ExerciseAttempt | null | undefined | ''> {
        return ajaxGet(`${API_URL}/api/exercise/getExistingExerciseAttempt?exerciseId=${exerciseId}`, TOptionalExerciseAttemptResult);
    }

    createExerciseAttempt(exerciseId: number): PromiseEither<RequestError, ExerciseAttempt> {
        return ajaxGet(`${API_URL}/api/exercise/createExerciseAttempt?exerciseId=${exerciseId}`, TExerciseAttempt); 
    }

    createDebugExerciseAttempt(exerciseId: number): PromiseEither<RequestError, ExerciseAttempt> {
        return ajaxGet(`${API_URL}/api/exercise/createDebugExerciseAttempt?exerciseId=${exerciseId}`, TExerciseAttempt); 
    }

    getExerciseStatistics(exerciseId: number): PromiseEither<RequestError, ExerciseStatisticsItem[]> {
        return ajaxGet(`${API_URL}/api/exercise/getExerciseStatistics?exerciseId=${exerciseId}`, TExerciseStatisticsItems)
    }

    getExercises(): PromiseEither<RequestError, number[]> {
        return ajaxGet(`${API_URL}/api/exercise/getExercises`, io.array(io.number));
    }
}
