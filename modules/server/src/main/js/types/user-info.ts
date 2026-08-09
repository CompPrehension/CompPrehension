import * as io from 'io-ts'
import { Language, TLanguage } from './language'

export type UserInfo = {
    id: number,
    displayName: string,
    email: string | null,
    language: Language,
    permissions: string[],
}
export const TUserInfo : io.Type<UserInfo> = io.type({
    id: io.number,
    displayName: io.string,
    email: io.union([io.string, io.null]),
    language: TLanguage,
    permissions: io.array(io.string),
}, 'UserInfo')
