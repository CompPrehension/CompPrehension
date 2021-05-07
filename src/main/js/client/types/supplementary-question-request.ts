import { NonEmptyArray } from 'fp-ts/lib/NonEmptyArray'

export type SupplementaryQuestionRequest = {
    exerciseAttemptId: number,
    violations: NonEmptyArray<number>,
}
