
import * as io from 'io-ts'
import { Answer, TAnswer } from './answer';

export type Interaction = {
    questionId: number,
    answers: Answer[],    
}
export const TInteraction : io.Type<Interaction> = io.type({
    questionId: io.number,
    answers: io.array(TAnswer),
}, 'Interaction');
