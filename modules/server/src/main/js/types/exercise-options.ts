import * as io from 'io-ts'

/** Values of strategy settings as stored in the exercise: nested groups of flags, numbers and choices. */
export type StrategySettingValues = { [name: string]: boolean | number | string | StrategySettingValues }
export const TStrategySettingValues: io.Type<StrategySettingValues> = io.recursion('StrategySettingValues', () =>
    io.record(io.string, io.union([io.boolean, io.number, io.string, TStrategySettingValues])))

export type ExerciseOptions = {
    surveyOptions?: {
        enabled: boolean,
        surveyId: string,
    },
    forceNewAttemptCreationEnabled: boolean,
    newQuestionGenerationEnabled: boolean,
    supplementaryQuestionsEnabled: boolean,
    correctAnswerGenerationEnabled: boolean,
    debugButtonEnabled: boolean,
    maxExpectedConcurrentStudents: number,
    strategySettings?: StrategySettingValues,
}
export const TExerciseOptions: io.Type<ExerciseOptions> = io.intersection([
    io.type({
        forceNewAttemptCreationEnabled: io.boolean,
        debugButtonEnabled: io.boolean,
        newQuestionGenerationEnabled: io.boolean,
        supplementaryQuestionsEnabled: io.boolean,
        correctAnswerGenerationEnabled: io.boolean,
        maxExpectedConcurrentStudents: io.number,
    }),
    io.partial({
        surveyOptions: io.type({
            enabled: io.boolean,
            surveyId: io.string,
        }),
        strategySettings: TStrategySettingValues,
    })
], 'ExerciseOptions');
