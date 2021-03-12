
import * as io from 'io-ts'

export const TInteraction = io.type({
    attemptId: io.number,
    questionId: io.number,
    answers: io.array(io.tuple([io.number, io.number])),
});
export type Interaction = io.TypeOf<typeof TInteraction>
