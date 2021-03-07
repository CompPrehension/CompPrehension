import { MatchingQuestionOptions, OrderQuestionOptions, QuestionOptions } from "./question-options";

export type QuestionType = "SINGLE_CHOICE" | "MULTI_CHOICE" | "MATCHING" | "ORDER";

export type Html = string;

export interface QuestionAnswer {
    id: number,
    text: Html,
}

type QuestionBase = {
    attemptId: number,
    questionId: number,
    type: QuestionType,
    options: QuestionOptions,
    text: Html,
    answers: QuestionAnswer[],
}

type OrderQuestionBase = QuestionBase & {        
    type: "ORDER",        
    options: OrderQuestionOptions,        
}

type SingleChoiceQuestionBase = QuestionBase & {        
    type: "SINGLE_CHOICE",            
}

type MultiChoiceQuestionBase = QuestionBase & {        
    type: "MULTI_CHOICE",            
}

type MathingQuestionBase = QuestionBase & {        
    type: "MATCHING",  
    answers: QuestionAnswer[],
    groups: QuestionAnswer[],
    options: MatchingQuestionOptions,
}

export type Question = OrderQuestionBase | SingleChoiceQuestionBase | MultiChoiceQuestionBase | MathingQuestionBase
