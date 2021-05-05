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

type ComboboxMatchingQuestionOptions = QuestionOptions & {
    multipleSelectionEnabled: boolean, 
    displayMode: 'combobox',
}
type DnDMatchingQuestionOptions = QuestionOptions & {
    multipleSelectionEnabled: boolean, 
    displayMode: 'dragNdrop',
    dropzoneStyle?: string,
}
export type MatchingQuestionOptions = ComboboxMatchingQuestionOptions | DnDMatchingQuestionOptions
export const TMatchingQuestionOptions: io.Type<MatchingQuestionOptions> = io.intersection([
    TQuestionOptions,
    io.type({
        multipleSelectionEnabled: io.boolean,
    }),
    io.union([
        io.type({
            displayMode: io.literal('combobox'),
        }),
        io.intersection([
            io.type({
                displayMode: io.literal('dragNdrop'),
            }),
            io.partial({
                dropzoneStyle: io.string,
            }),
        ]),
    ]),
], 'MatchingQuestionOptions')

export type SingleChoiceQuestionOptions = QuestionOptions & {
    displayMode: 'radio' | 'dragNdrop',
}
export const TSingleChoiceQuestionOptions: io.Type<SingleChoiceQuestionOptions> = io.intersection([
    TQuestionOptions,
    io.type({
        displayMode: io.keyof({
            'radio': null,
            'dragNdrop': null,
        }),        
    }),
], 'SingleChoiceQuestionOptions');

export type MultiChoiceQuestionOptions = QuestionOptions & {
    displayMode: 'switch' | 'dragNdrop',
    selectorReplacers?: [string, string],
}
export const TMultiChoiceQuestionOptions: io.Type<MultiChoiceQuestionOptions> = io.intersection([
    TQuestionOptions,
    io.type({
        displayMode: io.keyof({
            'switch': null,
            'dragNdrop': null,
        }),        
    }),
    io.partial({
        selectorReplacers: io.tuple([io.string, io.string]),
    }),
], 'MultiChoiceQuestionOptions')
