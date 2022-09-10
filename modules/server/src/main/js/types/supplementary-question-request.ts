import { NonEmptyArray } from 'fp-ts/lib/NonEmptyArray'

export type SupplementaryQuestionRequest = {
    exerciseAttemptId: number,
    questionId: number,
    violationLaws: NonEmptyArray<string>,
}
