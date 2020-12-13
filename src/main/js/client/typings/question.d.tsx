
export enum QuestionType {
    SINGLE_CHOICE,
    MULTI_CHOICE,
    MATCHING,
    ORDER,
}

type Html = string;

export type Question = {
    id: string,
    type: QuestionType,
    text: Html,    
    answers: QuestionAnswer[],
}

export type QuestionAnswer = {
    id: string,
    text: Html,
}

export type SessionInfo = {
    sessionId: string,
    questionId: string,
    user: UserInfo,
    expired: Date,
}

export type UserInfo = {
    id: string,
    displayName: string,
    email: string,
    roles: string[],
}

