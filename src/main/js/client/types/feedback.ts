import * as io from 'io-ts'
import { MergeIntersections } from './utils';

export type FeedbackSuccessMessage = {
    type: 'SUCCESS',
    message: string,
}
export type FeedbackErrorMessage = {
    type: 'ERROR',
    message: string,
    violationLaws: string[],
}

export type FeedbackMessage = FeedbackSuccessMessage | FeedbackErrorMessage
const TFeedbackMessage: io.Type<FeedbackMessage> = io.union([
    io.type({
        type: io.literal('SUCCESS'),
        message: io.string,
    }),
    io.type({
        type: io.literal('ERROR'),
        message: io.string,
        violationLaws: io.array(io.string),
    }),
]);

export type Feedback = {
    grade?: number | null,   
    correctAnswers?: [number, number][] | null,
    correctSteps?: number | null,
    stepsLeft?: number | null,
    stepsWithErrors?: number | null,
    messages?: FeedbackMessage[] | null,
} 
export const TFeedback: io.Type<Feedback> = io.partial({
    grade: io.union([io.number, io.null]),
    violations: io.union([io.array(io.number), io.null]),
    correctAnswers: io.union([io.array(io.tuple([io.number, io.number])), io.null]),
    correctSteps: io.union([io.number, io.null]),
    stepsLeft: io.union([io.number, io.null]),
    stepsWithErrors: io.union([io.number, io.null]),
    message: io.union([io.array(TFeedbackMessage), io.null]),
}, 'Feedback');

export type OrderQuestionFeedback = MergeIntersections<Feedback & {
    trace?: string[] | null,
}>
export const TOrderQuestionFeedback: io.Type<Feedback> = io.intersection([
    TFeedback,
    io.partial({
        trace: io.union([io.array(io.string), io.null]),
    }),
]);
