import { UserInfo } from "./user-info";

export interface SessionInfo {
    sessionId: string,
    attemptIds: string[],
    user: UserInfo,
    expired: Date,
}
