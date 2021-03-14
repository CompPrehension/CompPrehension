import * as io from 'io-ts'

export type UserInfo = {
    id: number,
    displayName: string,
    email: string,
    roles: string[],
}
export const TUserInfo : io.Type<UserInfo> = io.type({
    id: io.number,
    displayName: io.string,
    email: io.string,
    roles: io.array(io.string),
})
