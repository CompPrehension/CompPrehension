import * as io from 'io-ts'


export type Answer = {
    answer: [number, number];
    createdByUser: boolean;
}
export const TAnswer: io.Type<Answer> = io.type({
    answer: io.tuple([io.number, io.number]),
    createdByUser: io.boolean,
})
