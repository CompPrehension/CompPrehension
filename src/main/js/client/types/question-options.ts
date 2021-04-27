import * as io from 'io-ts'

export type QuestionOptions = {
    requireContext: boolean,
}
export const TQuestionOptions : io.Type<QuestionOptions> = io.type({
    requireContext: io.boolean,
}, 'QuestionOptions');

export type OrderQuestionOptions = QuestionOptions & {
    showTrace: boolean,
    multipleSelectionEnabled: boolean,
    requireAllAnswers: boolean,
    orderNumberOptions?: {
        delimiter: string,
        position: 'PREFIX' | 'SUFFIX' | 'NONE',
        replacers?: string[] | null,
    }
};
export const TOrderQuestionOptions : io.Type<OrderQuestionOptions> = io.intersection([
    TQuestionOptions,
    io.type({
        showTrace: io.boolean,
        multipleSelectionEnabled: io.boolean,
        requireAllAnswers: io.boolean,        
    }),
    io.partial({
        orderNumberOptions: io.intersection([
            io.type({
                delimiter: io.string,
                position: io.keyof({
                    'PREFIX': null,
                    'SUFFIX': null,
                    'NONE': null,
                }),                    
            }),
            io.partial({
                replacers: io.union([io.array(io.string), io.null]),
            })
        ]),
    })
], 'OrderQuestionOptions')

export type MatchingQuestionOptions = QuestionOptions & {
    displayMode: 'combobox' | 'dragNdrop',
    multipleSelectionEnabled: boolean,
}
export const TMatchingQuestionOptions : io.Type<MatchingQuestionOptions> = io.intersection([
    TQuestionOptions,
    io.type({
        displayMode: io.keyof({
            'combobox': null,
            'dragNdrop': null,
        }),
        multipleSelectionEnabled: io.boolean,
    }),
], 'MatchingQuestionOptions')
