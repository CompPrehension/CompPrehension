import * as io from 'io-ts'
import { Language, TLanguage } from './language'
import { Role, TRole } from './role'

export type UserInfo = {
    id: number,
    displayName: string,
    email: string | null,
    language: Language,
    roles: string[],
}
export const TUserInfo : io.Type<UserInfo> = io.type({
    id: io.number,
    displayName: io.string,
    email: io.union([io.string, io.null]),
    language: TLanguage,
    roles: io.array(io.string),
}, 'UserInfo')
