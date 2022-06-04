import * as io from 'io-ts'

export type ExerciseOptions = {
    surveyOptions?: {
        enabled: boolean,
        surveyId: string,
    },
    newQuestionGenerationEnabled: boolean,
    supplementaryQuestionsEnabled: boolean,
    correctAnswerGenerationEnabled: boolean,
}
export const TExerciseOptions: io.Type<ExerciseOptions> = io.intersection([
    io.type({
        newQuestionGenerationEnabled: io.boolean,
        supplementaryQuestionsEnabled: io.boolean,
        correctAnswerGenerationEnabled: io.boolean,
    }),
    io.partial({
        surveyOptions: io.type({
            enabled: io.boolean,
            surveyId: io.string,
        }),
    })
], 'ExerciseOptions');
