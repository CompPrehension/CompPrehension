import { TUserInfo, UserInfo } from "./user-info";
import * as io from 'io-ts'

export type SessionInfo = {
    sessionId: string,
    attemptIds: number[],
    user: UserInfo
};
export const TSessionInfo : io.Type<SessionInfo> = io.type({
    sessionId: io.string,
    attemptIds: io.array(io.number),
    user: TUserInfo,
});
