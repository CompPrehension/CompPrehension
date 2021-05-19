import * as io from 'io-ts'
import { MergeIntersections } from './utils';

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

type ComboboxMatchingQuestionOptions = MergeIntersections<QuestionOptions & {
    multipleSelectionEnabled: boolean, 
    displayMode: 'combobox',
}>
type DnDMatchingQuestionOptions = MergeIntersections<QuestionOptions & {
    multipleSelectionEnabled: boolean, 
    displayMode: 'dragNdrop',
    draggableStyle: string,
    dropzoneStyle: string,
    dropzoneHtml: string,
}>
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
        io.type({
            displayMode: io.literal('dragNdrop'),
            draggableStyle: io.string,
            dropzoneStyle: io.string,
            dropzoneHtml: io.string,
        }),
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


type SwitchMultiChoiceQuestionOptions = MergeIntersections<QuestionOptions & {
    displayMode: 'switch',
    selectorReplacers?: [string, string] | null,
}>
type DnDMultiChoiceQuestionOptions = MergeIntersections<QuestionOptions & {
    displayMode: 'dragNdrop',
    dropzoneHtml: string,
    dropzoneStyle: string,
    draggableStyle: string,
}>
export type MultiChoiceQuestionOptions = SwitchMultiChoiceQuestionOptions | DnDMultiChoiceQuestionOptions
export const TMultiChoiceQuestionOptions: io.Type<MultiChoiceQuestionOptions> = io.intersection([
    TQuestionOptions,
    io.union([
        io.intersection([
            io.type({
                displayMode: io.literal('switch'),
            }),
            io.partial({
                selectorReplacers: io.union([io.tuple([io.string, io.string]), io.null]),
            }),
        ]),
        io.type({
            displayMode: io.literal('dragNdrop'),
            dropzoneHtml: io.string,
            dropzoneStyle: io.string,
            draggableStyle: io.string,
        }),
    ]),
], 'MultiChoiceQuestionOptions')
