import * as io from 'io-ts'
import { Language, TLanguage } from './language'

export type UserPermissions = {
    canViewGlobalPool: boolean,
}
export const TUserPermissions: io.Type<UserPermissions> = io.type({
    canViewGlobalPool: io.boolean,
}, 'UserPermissions')

export type UserInfo = {
    id: number,
    displayName: string,
    email: string | null,
    language: Language,
    permissions: UserPermissions,
}
export const TUserInfo : io.Type<UserInfo> = io.type({
    id: io.number,
    displayName: io.string,
    email: io.union([io.string, io.null]),
    language: TLanguage,
    permissions: TUserPermissions,
}, 'UserInfo')
