
import * as io from 'io-ts'

export type Interaction = {
    attemptId: number,
    questionId: number,
    answers: [number, number][],    
}
export const TInteraction : io.Type<Interaction> = io.type({
    attemptId: io.number,
    questionId: io.number,
    answers: io.array(io.tuple([io.number, io.number])),
}, 'Interaction');
