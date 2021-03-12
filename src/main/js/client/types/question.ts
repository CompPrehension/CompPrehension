import { TMatchingQuestionOptions, TOrderQuestionOptions, TQuestionOptions } from "./question-options";
import * as io from 'io-ts'
import { MergeIntersections } from "./utils";

export const TQuestionType = io.union([io.literal("SINGLE_CHOICE"),io.literal("MULTI_CHOICE"),io.literal("MATCHING"),io.literal("ORDER"),])
export type QuestionType = io.TypeOf<typeof TQuestionType>;

export const THtml = io.string;
export type Html = io.TypeOf<typeof THtml>;

export const TQuestionAnswer = io.type({
    id: io.number,
    text: THtml,
});
export type QuestionAnswer = io.TypeOf<typeof TQuestionAnswer>;

const TQuestionBase = io.type({
    attemptId: io.number,
    questionId: io.number,
    type: TQuestionType,
    options: TQuestionOptions,
    text: THtml,
    answers: io.array(TQuestionAnswer),
});
type QuestionBase = io.TypeOf<typeof TQuestionBase>;

const TOrderQuestionBase = io.intersection([
    TQuestionBase,
    io.type({
        type: io.literal("ORDER"),
        options: TOrderQuestionOptions,
    })
])
type OrderQuestionBase = MergeIntersections<io.TypeOf<typeof TOrderQuestionBase>>

const TSingleChoiceQuestionBase = io.intersection([
    TQuestionBase,
    io.type({
        type: io.literal("SINGLE_CHOICE"),        
    })
])
type SingleChoiceQuestionBase = MergeIntersections<io.TypeOf<typeof TSingleChoiceQuestionBase>>

const TMultiChoiceQuestionBase = io.intersection([
    TQuestionBase,
    io.type({
        type: io.literal("MULTI_CHOICE"),        
    }),
])
type MultiChoiceQuestionBase = MergeIntersections<io.TypeOf<typeof TMultiChoiceQuestionBase>>

const TMatchingQuestionBase = io.intersection([
    TQuestionBase,
    io.type({
        type: io.literal("MATCHING"),        
        answers: io.array(TQuestionAnswer),
        groups: io.array(TQuestionAnswer),
        options: TMatchingQuestionOptions,
    }),
])
type MatchingQuestionBase = MergeIntersections<io.TypeOf<typeof TMatchingQuestionBase>>

export const TQuestion = io.union([TOrderQuestionBase, TSingleChoiceQuestionBase, TMultiChoiceQuestionBase, TMatchingQuestionBase])
export type Question = MergeIntersections<io.TypeOf<typeof TQuestion>>
