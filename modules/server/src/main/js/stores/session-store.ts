import * as E from "fp-ts/Either";
import i18next from "i18next";
import { makeAutoObservable } from "mobx";
import { userController } from "../controllers";
import { Language } from "../types/language";
import { RequestError } from "../types/request-error";
import { UserInfo } from "../types/user-info";


export class SessionStore {
    user?: UserInfo = undefined;
    languages: string[] = [];
    isSessionLoading: boolean = false;
    error: RequestError | null = null;

    get selectedLanguage(): string | undefined {
        return this.user?.language;
    }
    get isSessionLoaded(): boolean {
        return this.user !== undefined;
    }

    constructor() {
        makeAutoObservable(this);
    }

    loadSessionInfo = async () => {
        if (this.isSessionLoading) {
            return;
        }

        this.isSessionLoading = true;
        this.error = null;

        const [user, languages] = await Promise.all([
            userController.getCurrentUser(),
            userController.getLanguages(),
        ]);

        this.isSessionLoading = false;

        // without a session there is nothing to render, so the failure has to be kept:
        // otherwise every page sits on its loading spinner forever
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
    };

    changeLanguage = async (newLang: Language) => {
        if (!this.user || this.user.language === newLang) {
            return;
        }

        const res = await userController.setLanguage(newLang);
        if (E.isLeft(res)) {
            console.error("Failed to change language", res.left);
            return;
        }

        this.user.language = res.right;
        i18next.changeLanguage(res.right);
    }
}
