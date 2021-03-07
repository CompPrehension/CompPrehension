import { UserInfo } from "./user-info";

export interface SessionInfo {
    sessionId: string,
    attemptIds: number[],
    user: UserInfo,
    expired: Date,
}
