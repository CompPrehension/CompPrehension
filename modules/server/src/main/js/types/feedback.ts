import * as io from 'io-ts'
import { Answer, TAnswer } from './answer';
import { MergeIntersections } from './utils';

export type FeedbackViolationLaw = {
    name: string,
    canCreateSupplementaryQuestion: boolean,
}
const TFeedbackViolationLaw: io.Type<FeedbackViolationLaw> = io.type({
    name: io.string,
    canCreateSupplementaryQuestion: io.boolean,
})

export type FeedbackSuccessMessage = {
    type: 'SUCCESS',
    message: string,
    violationLaw: FeedbackViolationLaw,
}
export type FeedbackErrorMessage = {
    type: 'ERROR',
    message: string,
    violationLaw: FeedbackViolationLaw,
}

export type FeedbackMessage = FeedbackSuccessMessage | FeedbackErrorMessage
const TFeedbackMessage: io.Type<FeedbackMessage> = io.union([
    io.type({
        type: io.literal('SUCCESS'),
        message: io.string,
        violationLaw: TFeedbackViolationLaw,
    }),
    io.type({
        type: io.literal('ERROR'),
        message: io.string,
        violationLaw: TFeedbackViolationLaw,
    }),
])

export type Feedback = {
    isCorrect: boolean,
    grade?: number | null,   
    correctAnswers?: Answer[] | null,
    correctSteps?: number | null,
    stepsLeft?: number | null,
    stepsWithErrors?: number | null,
    messages?: FeedbackMessage[] | null,
    strategyDecision?: 'CONTINUE' | 'FINISH' | null,
} 
export const TFeedback: io.Type<Feedback> = io.intersection([
    io.type({
        isCorrect: io.boolean,
    }),
    io.partial({
        isCorrect: io.boolean,
        grade: io.union([io.number, io.null]),
        violations: io.union([io.array(io.number), io.null]),
        correctAnswers: io.union([io.array(TAnswer), io.null]),
        correctSteps: io.union([io.number, io.null]),
        stepsLeft: io.union([io.number, io.null]),
        stepsWithErrors: io.union([io.number, io.null]),
        message: io.union([io.array(TFeedbackMessage), io.null]),
        strategyDecision: io.union([
            io.keyof({
                'CONTINUE': null,
                'FINISH': null,
            }),
            io.null,
        ]),
    }),
], 'Feedback');


export type OrderQuestionFeedback = MergeIntersections<Feedback & {
    trace?: string[] | null,
}>
export const TOrderQuestionFeedback: io.Type<Feedback> = io.intersection([
    TFeedback,
    io.partial({
        trace: io.union([io.array(io.string), io.null]),
    }),
]);
