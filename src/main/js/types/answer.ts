import * as io from 'io-ts'


export type Answer = {
    answer: [number, number];
    isCreatedByUser: boolean;
}
export const TAnswer: io.Type<Answer> = io.type({
    answer: io.tuple([io.number, io.number]),
    isCreatedByUser: io.boolean,
})
