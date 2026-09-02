import { ExerciseAttempt, TExerciseAttempt, TOptionalExerciseAttemptResult } from "../../types/exercise-attempt";
import { ExerciseStatisticsItem, TExerciseStatisticsItems } from "../../types/exercise-statistics";
import { ajaxGet, PromiseEither } from "../../utils/ajax";
import * as io from 'io-ts'
import { RequestError } from "../../types/request-error";
import { API_URL } from "../../appconfig";
import { Exercise, TExercise } from "../../types/exercise";

export class ExerciseController {

    getExerciseShortInfo(id: number, courseId?: number): PromiseEither<RequestError, Exercise> {
        const courseParam = courseId != null ? `&courseId=${courseId}` : '';
        return ajaxGet(`${API_URL}/api/exercise/shortInfo?id=${id}${courseParam}`, TExercise);
    }

    getExerciseAttempt(attemptId: number): PromiseEither<RequestError, ExerciseAttempt> {
        return ajaxGet(`${API_URL}/api/exercise/getExerciseAttempt?attemptId=${attemptId}`, TExerciseAttempt);
    }

    getExistingExerciseAttempt(exerciseId: number, courseId?: number): PromiseEither<RequestError, ExerciseAttempt | null | undefined | ''> {
        const courseParam = courseId != null ? `&courseId=${courseId}` : '';
        return ajaxGet(`${API_URL}/api/exercise/getExistingExerciseAttempt?exerciseId=${exerciseId}${courseParam}`, TOptionalExerciseAttemptResult);
    }

    createExerciseAttempt(exerciseId: number, courseId?: number): PromiseEither<RequestError, ExerciseAttempt> {
        const courseParam = courseId != null ? `&courseId=${courseId}` : '';
        return ajaxGet(`${API_URL}/api/exercise/createExerciseAttempt?exerciseId=${exerciseId}${courseParam}`, TExerciseAttempt);
    }

    createDebugExerciseAttempt(exerciseId: number, courseId?: number): PromiseEither<RequestError, ExerciseAttempt> {
        const courseParam = courseId != null ? `&courseId=${courseId}` : '';
        return ajaxGet(`${API_URL}/api/exercise/createDebugExerciseAttempt?exerciseId=${exerciseId}${courseParam}`, TExerciseAttempt);
    }

    getExerciseStatistics(exerciseId: number): PromiseEither<RequestError, ExerciseStatisticsItem[]> {
        return ajaxGet(`${API_URL}/api/exercise/getExerciseStatistics?exerciseId=${exerciseId}`, TExerciseStatisticsItems)
    }

    getExercises(): PromiseEither<RequestError, number[]> {
        return ajaxGet(`${API_URL}/api/exercise/getExercises`, io.array(io.number));
    }
}
