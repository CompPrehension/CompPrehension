
export type QuestionType = "SINGLE_CHOICE" | "MULTI_CHOICE" | "MATCHING" | "ORDER";

export type Html = string;


type QuestionBase = {
    id: string,
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
}

export type Question = OrderQuestionBase | SingleChoiceQuestionBase | MultiChoiceQuestionBase | MathingQuestionBase
      
export interface QuestionOptions {
    requireContext: boolean,
    showTrace: boolean;
}

export interface OrderQuestionOptions extends QuestionOptions {
    disableOnSelected: boolean,
    showOrderNumbers: boolean, 
    orderNumberSuffix?: string,
    orderNumberReplacers?: string[],
}

export interface QuestionAnswer {
    id: string,
    text: Html,
}

export interface SessionInfo {
    sessionId: string,
    attemptIds: string[],
    user: UserInfo,
    expired: Date,
}

export interface UserInfo {
    id: string,
    displayName: string,
    email: string,
    roles: string[],
}

