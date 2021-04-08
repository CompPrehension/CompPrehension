import * as io from 'io-ts'
import { TOptionalRequestResult } from './utils';

export type ExerciseAttempt = {
    attemptId: number,
    exerciseId: number,
    questionIds: number[],    
};
export const TExerciseAttempt : io.Type<ExerciseAttempt> = io.type({
    attemptId: io.number,
    exerciseId: io.number,
    questionIds: io.array(io.number),
});
export const TOptionalExerciseAttemptResult = TOptionalRequestResult(TExerciseAttempt);
