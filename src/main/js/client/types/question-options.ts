import * as io from 'io-ts'
import { MergeIntersections } from './utils'

export const TQuestionOptions = io.type({
    requireContext: io.boolean,
})
export type QuestionOptions = io.TypeOf<typeof TQuestionOptions>

export const TOrderQuestionOptions = io.intersection([
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
])
export type OrderQuestionOptions = MergeIntersections<io.TypeOf<typeof TOrderQuestionOptions>>

export const TMatchingQuestionOptions = io.intersection([
    TQuestionOptions,
    io.type({
        displayMode: io.keyof({
            'combobox': null,
            'dragNdrop': null,
        }),
    }),
])
export type MatchingQuestionOptions = MergeIntersections<io.TypeOf<typeof TMatchingQuestionOptions>>
