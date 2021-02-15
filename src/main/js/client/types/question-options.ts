
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
