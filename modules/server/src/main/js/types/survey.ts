import * as io from 'io-ts'

const TSurveyQuestionTriggeringPolicy = io.union([
    io.type({
        kind: io.literal('AFTER_FIRST'),
    }),
    io.type({
        kind: io.literal('AFTER_LAST'),
    }),
    io.type({
        kind: io.literal('AFTER_EACH'),
    }),
    io.type({
        kind: io.literal('AFTER_SPECIFIC'),
        numbers: io.array(io.number),
    }),
])
export type SurveyQuestionTriggeringPolicy = io.TypeOf<typeof TSurveyQuestionTriggeringPolicy>

const TYesNoSurveyQuestion = io.type({
    id: io.number,
    type: io.literal('yes-no'),
    text: io.string,
    policy: TSurveyQuestionTriggeringPolicy,
    required: io.boolean,
    options: io.type({
        yesText: io.string,
        yesValue: io.string,
        noText: io.string,
        noValue: io.string,
    }),
}, 'YesNoSurveyQuestion')
export type YesNoSurveyQuestion = io.TypeOf<typeof TYesNoSurveyQuestion>

const TSingleChoiceSurveyQuestion = io.type({
    id: io.number,
    type: io.literal('single-choice'),
    text: io.string,
    policy: TSurveyQuestionTriggeringPolicy,
    required: io.boolean,
    options:  io.array(io.type({
        id: io.string,
        text: io.string,            
    })),
})
export type SingleChoiceSurveyQuestion = io.TypeOf<typeof TSingleChoiceSurveyQuestion>

const TOpenEndedSurveyQuestion = io.type({
    id: io.number,
    type: io.literal('open-ended'),
    text: io.string,
    policy: TSurveyQuestionTriggeringPolicy,
    required: io.boolean,
})
export type OpenEndedSurveyQuestion = io.TypeOf<typeof TOpenEndedSurveyQuestion>

const TSurveyQuestion: io.Type<SurveyQuestion> = io.union([TYesNoSurveyQuestion, TSingleChoiceSurveyQuestion, TOpenEndedSurveyQuestion])
export type SurveyQuestion = YesNoSurveyQuestion | SingleChoiceSurveyQuestion | OpenEndedSurveyQuestion

export type Survey = {
    surveyId: string,
    options: {
    },
    questions: SurveyQuestion[],
}
export const TSurvey : io.Type<Survey> = io.type({
    surveyId: io.string,
    options: io.type({
    }),
    questions: io.array(TSurveyQuestion),    
}, 'Survey')


export const TSurveyResultItem = io.type({
    surveyQuestionId: io.number, 
    questionId: io.number, 
    answer: io.string,
})
export type SurveyResultItem = io.TypeOf<typeof TSurveyResultItem>
