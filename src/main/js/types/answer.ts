import * as io from 'io-ts'


export type Answer = {
    answer: [number, number];
    isСreatedByUser: boolean;
    createdByInteraction?: number | null;
}
export const TAnswer: io.Type<Answer> = io.intersection([
    io.type({
        answer: io.tuple([io.number, io.number]),
        isСreatedByUser: io.boolean,
    }),
    io.partial({
        createdByInteraction: io.union([io.number, io.null]),
    }),
])

