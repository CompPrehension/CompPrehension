import * as io from 'io-ts'

export type ExerciseOptions = {
    surveyOptions?: {
        enabled: boolean,
        surveyId: string,
    },
    forceNewAttemptCreationEnabled: boolean,
    newQuestionGenerationEnabled: boolean,
    supplementaryQuestionsEnabled: boolean,
    correctAnswerGenerationEnabled: boolean,
    preferDecisionTreeBasedSupplementaryEnabled: boolean,
    maxExpectedConcurrentStudents: number,
}
export const TExerciseOptions: io.Type<ExerciseOptions> = io.intersection([
    io.type({
        forceNewAttemptCreationEnabled: io.boolean,
        newQuestionGenerationEnabled: io.boolean,
        supplementaryQuestionsEnabled: io.boolean,
        correctAnswerGenerationEnabled: io.boolean,
        preferDecisionTreeBasedSupplementaryEnabled: io.boolean,
        maxExpectedConcurrentStudents: io.number,
    }),
    io.partial({
        surveyOptions: io.type({
            enabled: io.boolean,
            surveyId: io.string,
        }),
    })
], 'ExerciseOptions');
