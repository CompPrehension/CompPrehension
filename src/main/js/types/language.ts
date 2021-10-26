import * as io from 'io-ts'

export type Language = "EN" | "RU" | "PL"
export const TLanguage = io.keyof({
    EN: null,
    RU: null,
    PL: null,
});
