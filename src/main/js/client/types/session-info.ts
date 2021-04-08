import { TUserInfo, UserInfo } from "./user-info";
import * as io from 'io-ts'

export type SessionInfo = {
    sessionId: string,
    user: UserInfo,
    language: string,
};
export const TSessionInfo : io.Type<SessionInfo> = io.type({
    sessionId: io.string,
    user: TUserInfo,
    language: io.string,
});
