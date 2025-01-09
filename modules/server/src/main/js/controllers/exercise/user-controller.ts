import { injectable } from "tsyringe";
import { API_URL } from "../../appconfig";
import { RequestError } from "../../types/request-error";
import { UserInfo, TUserInfo } from "../../types/user-info";
import { PromiseEither, ajaxGet } from "../../utils/ajax";

export interface IUserController {
    getCurrentUser(): PromiseEither<RequestError, UserInfo>;
}

@injectable()
export class UserController implements IUserController {
    getCurrentUser(): PromiseEither<RequestError, UserInfo> {
        return ajaxGet(`${API_URL}/api/users/whoami`, TUserInfo);
    }
}
