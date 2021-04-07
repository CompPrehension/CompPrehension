import * as io from 'io-ts'

export type ExerciseStatisticsItem = {
    attemptId: number;
    questionsCount: number;
    totalInteractionsCount: number;
    totalInteractionsWithErrorsCount: number;
    averageGrade: number;
}
export const TExerciseStatisticsItem: io.Type<ExerciseStatisticsItem> = io.type({
    attemptId: io.number,
    questionsCount: io.number,
    totalInteractionsCount: io.number,
    totalInteractionsWithErrorsCount: io.number,
    averageGrade: io.number,
})
export const TExerciseStatisticsItems: io.Type<ExerciseStatisticsItem[]> = io.array(TExerciseStatisticsItem);

