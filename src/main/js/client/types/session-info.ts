import { TUserInfo } from "./user-info";
import * as io from 'io-ts'

export const TSessionInfo = io.type({
    sessionId: io.string,
    attemptIds: io.array(io.number),
    user: TUserInfo,
});
export type SessionInfo = io.TypeOf<typeof TSessionInfo>;
