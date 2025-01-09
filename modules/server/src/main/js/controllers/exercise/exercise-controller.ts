import { injectable } from "tsyringe";
import { ExerciseAttempt, TExerciseAttempt, TOptionalExerciseAttemptResult } from "../../types/exercise-attempt";
import { ExerciseStatisticsItem, TExerciseStatisticsItems } from "../../types/exercise-statistics";
import { ajaxGet, PromiseEither } from "../../utils/ajax";
import * as io from 'io-ts'
import { RequestError } from "../../types/request-error";
import { API_URL } from "../../appconfig";
import { Exercise, TExercise } from "../../types/exercise";


export interface IExerciseController {
    getExerciseShortInfo(id: number): PromiseEither<RequestError, Exercise>;    
    getExistingExerciseAttempt(exerciseId: number): PromiseEither<RequestError, ExerciseAttempt | null | undefined | ''>;
    getExerciseAttempt(attemptId: number): PromiseEither<RequestError, ExerciseAttempt>;
    createDebugExerciseAttempt(exerciseId: number): PromiseEither<RequestError, ExerciseAttempt>;
    createExerciseAttempt(exerciseId: number): PromiseEither<RequestError, ExerciseAttempt>;
    getExerciseStatistics(exerciseId: number): PromiseEither<RequestError, ExerciseStatisticsItem[]>;
    getExercises(): PromiseEither<RequestError, number[]>
}

@injectable()
export class ExerciseController implements IExerciseController {

    getExerciseShortInfo(id: number): PromiseEither<RequestError, Exercise> {
        return ajaxGet(`${API_URL}/api/exercise/shortInfo?id=${id}`, TExercise);
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
