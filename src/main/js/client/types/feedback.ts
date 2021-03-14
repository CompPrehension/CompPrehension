import * as io from 'io-ts'

export type Feedback = {
    grade: number,
    errors: string[],
    iterationsLeft: number,
    correctOptionsCount: number,
} 
export const TFeedback : io.Type<Feedback> = io.type({
    grade: io.number,
    errors: io.array(io.string),
    iterationsLeft: io.number,
    correctOptionsCount: io.number,
});




