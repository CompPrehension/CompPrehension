import * as io from 'io-ts'

export type ExerciseOptions = {
    newQuestionGenerationEnabled: boolean,
    supplementaryQuestionsEnabled: boolean,
    correctAnswerGenerationEnabled: boolean,
}
export const TExerciseOptions: io.Type<ExerciseOptions> = io.type({
    newQuestionGenerationEnabled: io.boolean,
    supplementaryQuestionsEnabled: io.boolean,
    correctAnswerGenerationEnabled: io.boolean,
}, 'ExerciseOptions');
