import * as io from 'io-ts';
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
    violationLaws: FeedbackViolationLaw[] | null,
}
export type FeedbackErrorMessage = {
    type: 'ERROR',
    message: string,
    violationLaws: FeedbackViolationLaw[] | null,
}

export type FeedbackMessage = FeedbackSuccessMessage | FeedbackErrorMessage
export const TFeedbackMessage: io.Type<FeedbackMessage> = io.union([
    io.type({
            type: io.literal('SUCCESS'),
            message: io.string,
            violationLaws: io.union([io.array(TFeedbackViolationLaw), io.null])
        }),    
    io.type({
        type: io.literal('ERROR'),
        message: io.string,
        violationLaws: io.union([io.array(TFeedbackViolationLaw), io.null]),
    }),
])

/** A question about the reasoning behind an answer that several misconceptions explain. */
export type Clarification = {
    prompt: string,
    // Options are told apart by id: two of them may share a hypothesis.
    options: { id: number, hypothesis: string, reason: string }[],
}
const TClarification: io.Type<Clarification> = io.type({
    prompt: io.string,
    options: io.array(io.type({ id: io.number, hypothesis: io.string, reason: io.string })),
})

/** No option means the student named another reason. */
export type ClarificationAnswer = {
    questionId: number,
    option: number | null,
}

export type ClarificationFeedback = {
    explanation: string | null,
}
export const TClarificationFeedback: io.Type<ClarificationFeedback> = io.type({
    explanation: io.union([io.string, io.null]),
}, 'ClarificationFeedback')

export type Feedback = {
    isCorrect: boolean,
    grade?: number | null,   
    correctAnswers?: Answer[] | null,
    correctSteps?: number | null,
    stepsLeft?: number | null,
    stepsWithErrors?: number | null,
    messages?: FeedbackMessage[] | null,
    strategyDecision?: 'CONTINUE' | 'FINISH' | null,
    clarification?: Clarification | null,
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
        clarification: io.union([TClarification, io.null]),
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
