import * as io from 'io-ts'

export type Feedback = {
    grade: number | null,
    errors: string[] | null,
    correctSteps: number | null,
    stepsLeft: number | null,
    stepsWithErrors: number | null,
} 
export const TFeedback : io.Type<Feedback> = io.type({
    grade: io.union([io.number, io.null]),
    errors: io.union([io.array(io.string), io.null]),
    correctSteps: io.union([io.number, io.null]),
    stepsLeft: io.union([io.number, io.null]),
    stepsWithErrors: io.union([io.number, io.null]),
});




