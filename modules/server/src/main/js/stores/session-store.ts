import { computed, makeObservable, observable, runInAction } from "mobx";
import { UserInfo } from "../types/user-info";
import { UserController } from "../controllers/exercise/user-controller";
import * as E from "fp-ts/Either";
import i18next from "i18next";
import { Language } from "../types/language";


export class SessionStore {
    @observable user?: UserInfo = undefined;
    @observable languages: string[] = [];
    @computed selectedLanguage?: string = this.user?.language ?? undefined;
    @computed isSessionLoaded: boolean = this.user !== undefined;
    @observable isSessionLoading: boolean = false;
    private usersApi = new UserController()

    public SessionStore() {
        makeObservable(this);
    }

    loadSessionInfo = async () => {
        if (this.isSessionLoading) {
            return;
        }

        runInAction(() => {                
            this.isSessionLoading = true;
        })

        const [user, languages] = await Promise.all([
            this.usersApi.getCurrentUser(),
            this.usersApi.getLanguages(),
        ])
        
        if (E.isRight(user) && E.isRight(languages)) {
            runInAction(() => {
                this.isSessionLoading = false;
                this.user = user.right;
                this.languages = languages.right;

                if (this.user.language !== i18next.language) {
                    i18next.changeLanguage(this.user.language);
                }
            })
        }
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
