import * as io from 'io-ts'

export const TFeedback = io.type({
    grade: io.number,
    errors: io.array(io.string),
    iterationsLeft: io.number,
    correctOptionsCount: io.number,
})
export type Feedback = io.TypeOf<typeof TFeedback>


