import * as io from 'io-ts'

const TYesNoSurveyQuestion = io.type({
    id: io.number,
    type: io.literal('yes-no'),
    text: io.string,
    options: io.type({
        yesText: io.string,
        yesValue: io.string,
        noText: io.string,
        noValue: io.string,
    }),
}, 'YesNoSurveyQuestion')
export type YesNoSurveyQuestion = io.TypeOf<typeof TYesNoSurveyQuestion>

export type Survey = {
    surveyId: string,
    options: {
        size: number,
    },
    questions: YesNoSurveyQuestion[],
}
export const TSurvey : io.Type<Survey> = io.type({
    surveyId: io.string,
    options: io.type({
        size: io.number,
    }),
    questions: io.array(TYesNoSurveyQuestion),    
}, 'Survey')
