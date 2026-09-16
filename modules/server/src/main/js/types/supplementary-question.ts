import { NonEmptyArray } from 'fp-ts/lib/NonEmptyArray'
import * as io from 'io-ts'
import { FeedbackMessage, TFeedbackMessage } from './feedback';
import { TQuestion } from './question';

export type SupplementaryQuestionRequest = {
    questionId: number,
    violationLaws: NonEmptyArray<string>,
}

export type SupplementaryFeedbackAction = 'CONTINUE_AUTO' | 'CONTINUE_MANUAL' | 'FINISH';
const TSupplementaryFeedbackAction: io.Type<SupplementaryFeedbackAction> = io.keyof({
    'CONTINUE_AUTO': null,
    'CONTINUE_MANUAL': null,
    'FINISH': null,
});

export type SupplementaryFeedback = {
    message: FeedbackMessage,
    action: SupplementaryFeedbackAction,
}
export const TSupplementaryFeedback: io.Type<SupplementaryFeedback> = io.type({
    message: TFeedbackMessage,
    action: TSupplementaryFeedbackAction,
});

export const TSupplementaryQuestion = io.union([
    io.type({
        kind: io.literal("QUESTION"),
        question: TQuestion,
    }),
    io.type({
        kind: io.literal("FEEDBACK"),
        feedback: TSupplementaryFeedback,
    }),
]);
export type SupplementaryQuestion = io.TypeOf<typeof TSupplementaryQuestion>;

