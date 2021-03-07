
export interface QuestionOptions {
    requireContext: boolean,    
}

export interface OrderQuestionOptions extends QuestionOptions {
    showTrace: boolean;
    multipleSelectionEnabled: boolean,
    requireAllAnswers: boolean,
    orderNumberOptions?: {
        delimiter: string,
        position: 'PREFIX' | 'SUFFIX' | 'NONE',   
        replacers?: string[],         
    },    
}

export interface MatchingQuestionOptions extends QuestionOptions {
    displayMode: 'combobox' | 'dragNdrop',
}
