
import * as io from 'io-ts'
import { Answer, TAnswer } from './answer';

export type Interaction = {
    attemptId: number,
    questionId: number,
    answers: Answer[],    
}
export const TInteraction : io.Type<Interaction> = io.type({
    attemptId: io.number,
    questionId: io.number,
    answers: io.array(TAnswer),
}, 'Interaction');
