import { computed, makeObservable, observable, runInAction } from "mobx";
import { UserInfo } from "../types/user-info";
import { UserController } from "../controllers/exercise/user-controller";
import * as E from "fp-ts/Either";
import i18next from "i18next";
import { Language } from "../types/language";
import { RequestError } from "../types/request-error";


export class SessionStore {
    @observable user?: UserInfo = undefined;
    @observable languages: string[] = [];
    @observable isSessionLoading: boolean = false;
    @observable error: RequestError | null = null;
    private usersApi = new UserController()

    @computed get selectedLanguage(): string | undefined {
        return this.user?.language;
    }
    @computed get isSessionLoaded(): boolean {
        return this.user !== undefined;
    }

    constructor() {
        makeObservable(this);
    }

    loadSessionInfo = async () => {
        if (this.isSessionLoading) {
            return;
        }

        runInAction(() => {
            this.isSessionLoading = true;
            this.error = null;
        })

        const [user, languages] = await Promise.all([
            this.usersApi.getCurrentUser(),
            this.usersApi.getLanguages(),
        ])

        runInAction(() => {
            this.isSessionLoading = false;

            if (E.isLeft(user)) {
                this.error = user.left;
                return;
            }
            if (E.isLeft(languages)) {
                this.error = languages.left;
                return;
            }

            this.user = user.right;
            this.languages = languages.right;

            if (this.user.language !== i18next.language) {
                i18next.changeLanguage(this.user.language);
            }
        })
    };

    changeLanguage = async (newLang: Language) => {
        if (this.user && this.user.language !== newLang) {
            const res = await this.usersApi.setLanguage(newLang);
            if (E.isRight(res)) {
                runInAction(() => {
                    this.user!.language = res.right;
                    i18next.changeLanguage(res.right);
                });
            } else {
                console.error("Failed to change language", res.left);
            }
        }
    }
}
