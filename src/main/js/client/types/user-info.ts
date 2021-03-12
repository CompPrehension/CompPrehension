import * as io from 'io-ts'

export const TUserInfo = io.type({
    id: io.number,
    displayName: io.string,
    email: io.string,
    roles: io.array(io.string),
})
export type UserInfo = io.TypeOf<typeof TUserInfo>
