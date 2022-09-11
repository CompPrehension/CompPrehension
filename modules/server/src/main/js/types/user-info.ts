import * as io from 'io-ts'
import { Role, TRole } from './role'

export type UserInfo = {
    id: number,
    displayName: string,
    email: string | null,
    //roles: Role[],
}
export const TUserInfo : io.Type<UserInfo> = io.type({
    id: io.number,
    displayName: io.string,
    email: io.union([io.string, io.null]),
    //roles: io.array(TRole),
}, 'UserInfo')
