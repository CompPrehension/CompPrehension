import { TUserInfo, UserInfo } from "./user-info";
import * as io from 'io-ts'

export type SessionInfo = {
    sessionId: string,
    attemptId: number,
    questionIds: number[],
    user: UserInfo,
    language: string,
};
export const TSessionInfo : io.Type<SessionInfo> = io.type({
    sessionId: io.string,
    attemptId: io.number,
    questionIds: io.array(io.number),
    user: TUserInfo,
    language: io.string,
});
