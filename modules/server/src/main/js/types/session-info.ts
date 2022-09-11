import { TUserInfo, UserInfo } from "./user-info";
import * as io from 'io-ts'
import { Exercise, TExercise } from "./exercise";
import { Language, TLanguage } from "./language";

export type SessionInfo = {
    sessionId: string,
    exercise: Exercise,
    user: UserInfo,
    language: Language,
};
export const TSessionInfo : io.Type<SessionInfo> = io.type({
    sessionId: io.string,
    exercise: TExercise,
    user: TUserInfo,
    language: TLanguage,
}, 'SessionInfo');
