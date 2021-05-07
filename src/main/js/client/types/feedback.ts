import * as io from 'io-ts'

export type Feedback = {
    grade: number | null,
    errors: string[] | null,
    violations: number[] | null,
    explanation: string | null,
    correctAnswers: [number, number][] | null,
    correctSteps: number | null,
    stepsLeft: number | null,
    stepsWithErrors: number | null,
} 
export const TFeedback : io.Type<Feedback> = io.type({
    grade: io.union([io.number, io.null]),
    errors: io.union([io.array(io.string), io.null]),
    violations: io.union([io.array(io.number), io.null]),
    explanation: io.union([io.string, io.null]),
    correctAnswers: io.union([io.array(io.tuple([io.number, io.number])), io.null]),
    correctSteps: io.union([io.number, io.null]),
    stepsLeft: io.union([io.number, io.null]),
    stepsWithErrors: io.union([io.number, io.null]),
}, 'Feedback');




