import { injectable } from "tsyringe";
import { API_URL } from "../../appconfig";
import { RequestError } from "../../types/request-error";
import { UserInfo, TUserInfo } from "../../types/user-info";
import { PromiseEither, ajaxGet, ajaxPost } from "../../utils/ajax";
import * as E from "fp-ts/lib/Either";
import { Language, TLanguage } from "../../types/language";

export interface IUserController {
    getCurrentUser(): PromiseEither<RequestError, UserInfo>;
}

@injectable()
export class UserController implements IUserController {
    getCurrentUser(): PromiseEither<RequestError, UserInfo> {
        return ajaxGet(`${API_URL}/api/users/whoami`, TUserInfo);
    }

    async getLanguages(): PromiseEither<RequestError, string[]> {
        return E.right(["EN", "RU"]);
    }

    async setLanguage(language: Language): PromiseEither<RequestError, Language> {
        return ajaxPost(`${API_URL}/api/users/language`, { language }, TLanguage, undefined, 'raw');
    }
}
