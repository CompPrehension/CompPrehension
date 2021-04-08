import { action, makeObservable, observable, runInAction } from "mobx";
import { Api } from "../api";
import { SessionInfo } from "../types/session-info";
import * as E from "fp-ts/lib/Either";

export class SessionStore {
    @observable isSessionLoading: boolean = false;
    @observable sessionInfo?: SessionInfo = undefined;

    constructor() {
        makeObservable(this);        
    }

    @action 
    loadSessionInfo = async (): Promise<void> => {
        if (this.sessionInfo) {
            throw new Error("Session exists");
        }
        if (this.isSessionLoading) {
            return;
        }

        this.isSessionLoading = true;
        const dataEither = await Api.loadSessionInfo();                                
        const data = E.getOrElseW(_ => undefined)(dataEither);

        runInAction(() => {
            this.isSessionLoading = false;
            this.sessionInfo = data;
        });
    }
}

export const sessionStore = new SessionStore();