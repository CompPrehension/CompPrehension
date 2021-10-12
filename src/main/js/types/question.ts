import { MatchingQuestionOptions, MultiChoiceQuestionOptions, OrderQuestionOptions, QuestionOptions, SingleChoiceQuestionOptions, TMatchingQuestionOptions, TMultiChoiceQuestionOptions, TOrderQuestionOptions, TQuestionOptions, TSingleChoiceQuestionOptions } from "./question-options";
import * as io from 'io-ts'
import { Feedback, OrderQuestionFeedback, TFeedback, TOrderQuestionFeedback } from "./feedback";
import { MergeIntersectionsDeep } from "./utils";
import { TOptionalRequestResult } from "../utils/helpers";
import { Answer, TAnswer } from "./answer";

export type QuestionType = 'SINGLE_CHOICE' | 'MULTI_CHOICE' | 'MATCHING' | 'ORDER'
export const TQuestionType : io.Type<QuestionType> = io.keyof({
    'SINGLE_CHOICE': null,
    'MULTI_CHOICE': null,
    'MATCHING': null,
    'ORDER': null,
}, 'QuestionType');

export const THtml = io.string;
export type Html = io.TypeOf<typeof THtml>;

export type QuestionAnswer = {
    id: number,
    text: Html,
}
export const TQuestionAnswer : io.Type<QuestionAnswer> = io.type({
    id: io.number,
    text: THtml,
}, 'QuestionAnswer');

type QuestionBase = {
    attemptId: number,
    questionId: number,
    type: QuestionType,
    options: QuestionOptions,
    text: Html,
    answers: QuestionAnswer[],
    responses: Answer[] | null,
    feedback: Feedback | null,
}
const TQuestionBase : io.Type<QuestionBase> = io.type({
    attemptId: io.number,
    questionId: io.number,
    type: TQuestionType,
    options: TQuestionOptions,
    text: THtml,
    answers: io.array(TQuestionAnswer),
    responses: io.union([io.array(TAnswer), io.null]),
    feedback: io.union([TFeedback, io.null]),
}, 'QuestionBase');

export type OrderQuestion = MergeIntersectionsDeep<QuestionBase & {
    type: 'ORDER',
    options: OrderQuestionOptions,
    initialTrace?: string[] | null,
    feedback: OrderQuestionFeedback | null,
}>
const TOrderQuestion : io.Type<OrderQuestion> = io.intersection([
    TQuestionBase,
    io.type({
        type: io.literal("ORDER"),
        options: TOrderQuestionOptions,
        feedback: io.union([TOrderQuestionFeedback, io.null]),
    }),
    io.partial({
        initialTrace: io.union([io.array(io.string), io.null]),
    }),
], 'OrderQuestion')

export type SingleChoiceQuestion = MergeIntersectionsDeep<QuestionBase & {
    type: 'SINGLE_CHOICE',
    options: SingleChoiceQuestionOptions,
}>
const TSingleChoiceQuestion : io.Type<SingleChoiceQuestion> = io.intersection([
    TQuestionBase,
    io.type({
        type: io.literal("SINGLE_CHOICE"),
        options: TSingleChoiceQuestionOptions,
    })
], 'SingleChoiceQuestion')

export type MultiChoiceQuestion = MergeIntersectionsDeep<QuestionBase & {
    type: 'MULTI_CHOICE',
    options: MultiChoiceQuestionOptions,
}>
const TMultiChoiceQuestion : io.Type<MultiChoiceQuestion> = io.intersection([
    TQuestionBase,
    io.type({
        type: io.literal("MULTI_CHOICE"),
        options: TMultiChoiceQuestionOptions,
    }),
], 'MultiChoiceQuestion')

export type MatchingQuestion = MergeIntersectionsDeep<QuestionBase & {
    type: 'MATCHING',
    answers: QuestionAnswer[],
    groups: QuestionAnswer[],
    options: MatchingQuestionOptions,
}>
const TMatchingQuestion : io.Type<MatchingQuestion> = io.intersection([
    TQuestionBase,
    io.type({
        type: io.literal("MATCHING"),        
        answers: io.array(TQuestionAnswer),
        groups: io.array(TQuestionAnswer),
        options: TMatchingQuestionOptions,
    }),
], 'MatchingQuestion')

export type Question = OrderQuestion | SingleChoiceQuestion | MultiChoiceQuestion | MatchingQuestion
export const TQuestion : io.Type<Question> = io.union([TOrderQuestion, TSingleChoiceQuestion, TMultiChoiceQuestion, TMatchingQuestion], 'Question')
export const TOptionalQuestion: io.Type<Question | null | undefined | ''> = TOptionalRequestResult(TQuestion);

