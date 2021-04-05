import * as io from 'io-ts'

export type Feedback = {
    grade: number | null,
    errors: string[] | null,
    totalSteps: number | null,
    stepsLeft: number | null,
} 
export const TFeedback : io.Type<Feedback> = io.type({
    grade: io.union([io.number, io.null]),
    errors: io.union([io.array(io.string), io.null]),
    totalSteps: io.union([io.number, io.null]),
    stepsLeft: io.union([io.number, io.null]),
});




