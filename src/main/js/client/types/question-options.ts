
export interface QuestionOptions {
    requireContext: boolean,    
}

export interface OrderQuestionOptions extends QuestionOptions {
    showTrace: boolean;
    enableMultipleSelection: boolean,
    orderNumberOptions?: {
        delimiter: string,
        position: 'PREFIX' | 'SUFFIX' | 'NONE',   
        replacers?: string[],         
    },    
}

export interface MatchingQuestionOptions extends QuestionOptions {
    hideSelected: boolean,
    mode: 'combobox' | 'dragNdrop',
}
