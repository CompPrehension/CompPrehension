import { TUserInfo, UserInfo } from "./user-info";
import * as io from 'io-ts'

export type SessionInfo = {
    sessionId: string,
    exerciseId: number,
    user: UserInfo,
    language: string,
};
export const TSessionInfo : io.Type<SessionInfo> = io.type({
    sessionId: io.string,
    exerciseId: io.number,
    user: TUserInfo,
    language: io.string,
});
