
export enum QuestionType {
    SINGLE_CHOICE,
    MULTI_CHOICE,
    MATCHING,
    ORDER,
}

type Html = string;

export interface Question {
    id: string,
    type: QuestionType,
    text: Html,    
    options: QuestionOptions,
    answers: QuestionAnswer[],
}

export interface QuestionOptions {
    requireContext: boolean,
    showTrace: boolean;
}

export interface OrderQuestionOptions extends QuestionOptions {
    disableOnSelected: boolean,
    showOrderNumbers: boolean, 
    orderNumberSuffix: string,
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

