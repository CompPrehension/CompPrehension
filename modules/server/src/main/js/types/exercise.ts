import * as io from 'io-ts'
import { ExerciseOptions, TExerciseOptions } from './exercise-options'

export type Exercise = {
    id: number,
    options: ExerciseOptions,
}
export const TExercise: io.Type<Exercise> = io.type({
    id: io.number,
    options: TExerciseOptions,
}, 'Exercise')
