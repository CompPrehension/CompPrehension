import * as io from 'io-ts'

/** Values of settings as stored in the exercise: nested groups of flags, numbers and choices. */
export type SettingValues = { [name: string]: boolean | number | string | SettingValues }
export const TSettingValues: io.Type<SettingValues> = io.recursion('SettingValues', () =>
    io.record(io.string, io.union([io.boolean, io.number, io.string, TSettingValues])))

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
    answerReaction?: SettingValues,
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
        answerReaction: TSettingValues,
    })
], 'ExerciseOptions');
