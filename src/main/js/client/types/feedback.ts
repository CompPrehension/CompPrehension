import * as io from 'io-ts'

export type Feedback = {
    grade: number,
    errors: string[],
    totalSteps: number | null,
    stepsLeft: number,
} 
export const TFeedback : io.Type<Feedback> = io.type({
    grade: io.number,
    errors: io.array(io.string),
    totalSteps: io.union([io.number, io.null]),
    stepsLeft: io.number,
});




