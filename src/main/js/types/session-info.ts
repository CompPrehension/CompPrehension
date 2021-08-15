import { TUserInfo, UserInfo } from "./user-info";
import * as io from 'io-ts'
import { Exercise, TExercise } from "./exercise";

export type SessionInfo = {
    sessionId: string,
    exercise: Exercise,
    user: UserInfo,
    language: "RU" | "EN",
};
export const TSessionInfo : io.Type<SessionInfo> = io.type({
    sessionId: io.string,
    exercise: TExercise,
    user: TUserInfo,
    language: io.keyof({
        RU: null,
        EN: null,
    }),
}, 'SessionInfo');
