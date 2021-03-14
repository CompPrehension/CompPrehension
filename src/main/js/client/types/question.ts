import { MatchingQuestionOptions, OrderQuestionOptions, QuestionOptions, TMatchingQuestionOptions, TOrderQuestionOptions, TQuestionOptions } from "./question-options";
import * as io from 'io-ts'

export type QuestionType = 'SINGLE_CHOICE' | 'MULTI_CHOICE' | 'MATCHING' | 'ORDER'
export const TQuestionType : io.Type<QuestionType> = io.keyof({
    'SINGLE_CHOICE': null,
    'MULTI_CHOICE': null,
    'MATCHING': null,
    'ORDER': null,
});

export const THtml = io.string;
export type Html = io.TypeOf<typeof THtml>;

export type QuestionAnswer = {
    id: number,
    text: Html,
}
export const TQuestionAnswer : io.Type<QuestionAnswer> = io.type({
    id: io.number,
    text: THtml,
});

type QuestionBase = {
    attemptId: number,
    questionId: number,
    type: QuestionType,
    options: QuestionOptions,
    text: Html,
    answers: QuestionAnswer[],
}
const TQuestionBase : io.Type<QuestionBase> = io.type({
    attemptId: io.number,
    questionId: io.number,
    type: TQuestionType,
    options: TQuestionOptions,
    text: THtml,
    answers: io.array(TQuestionAnswer),
});

type OrderQuestionBase = QuestionBase & {
    type: 'ORDER',
    options: OrderQuestionOptions,
}
const TOrderQuestionBase : io.Type<OrderQuestionBase> = io.intersection([
    TQuestionBase,
    io.type({
        type: io.literal("ORDER"),
        options: TOrderQuestionOptions,
    }),
])

type SingleChoiceQuestionBase = QuestionBase & {
    type: 'SINGLE_CHOICE',
}
const TSingleChoiceQuestionBase : io.Type<SingleChoiceQuestionBase> = io.intersection([
    TQuestionBase,
    io.type({
        type: io.literal("SINGLE_CHOICE"),        
    })
])

type MultiChoiceQuestionBase = QuestionBase & {
    type: 'MULTI_CHOICE',
}
const TMultiChoiceQuestionBase : io.Type<MultiChoiceQuestionBase> = io.intersection([
    TQuestionBase,
    io.type({
        type: io.literal("MULTI_CHOICE"),        
    }),
])

type MatchingQuestionBase = QuestionBase & {
    type: 'MATCHING',
    answers: QuestionAnswer[],
    groups: QuestionAnswer[],
    options: MatchingQuestionOptions,
}
const TMatchingQuestionBase : io.Type<MatchingQuestionBase> = io.intersection([
    TQuestionBase,
    io.type({
        type: io.literal("MATCHING"),        
        answers: io.array(TQuestionAnswer),
        groups: io.array(TQuestionAnswer),
        options: TMatchingQuestionOptions,
    }),
])

export type Question = OrderQuestionBase | SingleChoiceQuestionBase | MultiChoiceQuestionBase | MatchingQuestionBase
export const TQuestion : io.Type<Question> = io.union([TOrderQuestionBase, TSingleChoiceQuestionBase, TMultiChoiceQuestionBase, TMatchingQuestionBase])

