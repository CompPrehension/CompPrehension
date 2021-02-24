
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

export interface MatchingQuestionOptions extends QuestionOptions {
    hideSelected: boolean,
    mode: 'combobox' | 'dragNdrop',
}
