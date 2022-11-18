import { injectable } from "tsyringe";
import { Domain, ExerciseCard, ExerciseListItem, TDomain, TExerciseCard, TExerciseListItem } from "../../types/exercise-settings";
import { ajaxDelete, ajaxGet, ajaxPost, ajaxPut, PromiseEither } from "../../utils/ajax";
import * as io from 'io-ts';
import { RequestError } from "../../types/request-error";
import { API_URL } from "../../appconfig";
import { toJS } from "mobx";
import { TOptionalRequestResult, TOptionalRequestResultV } from "../../utils/helpers";


@injectable()
export class ExerciseSettingsController {
    static endpointPath: string = ExerciseSettingsController.initEndpointPath();
    private static initEndpointPath(): string {
        const matches = /^\/(?:.+\/(?<!\/pages\/))?/.exec(window.location.pathname)
        if (matches) {
            return `${API_URL}${matches[0].substring(1)}`;
        }
        return API_URL;
    }

    getAllExercises(): PromiseEither<RequestError, ExerciseListItem[]> {
        return ajaxGet(`${ExerciseSettingsController.endpointPath}exercise/all`, io.array(TExerciseListItem));
    }

    getExercise(id: number): PromiseEither<RequestError, ExerciseCard> {
        return ajaxGet(`${ExerciseSettingsController.endpointPath}exercise?id=${encodeURIComponent(id)}`, TExerciseCard);
    }

    saveExercise(card: ExerciseCard): PromiseEither<RequestError, void> {
        return ajaxPost(`${ExerciseSettingsController.endpointPath}exercise`, toJS(card));
    }

    createExercise(name: string, domainId: string, strategyId: string): PromiseEither<RequestError, number> {
        return ajaxPut(`${ExerciseSettingsController.endpointPath}exercise`, { name, domainId, strategyId }, io.number);
    }

    deleteExercise(id: number): PromiseEither<RequestError, void> {
        return ajaxDelete(`${ExerciseSettingsController.endpointPath}exercise?id=${encodeURIComponent(id)}`);
    }

    getStrategies() : PromiseEither<RequestError, string[]> {
        return ajaxGet(`${ExerciseSettingsController.endpointPath}refTables/strategies`, io.array(io.string));
    }

    getBackends() : PromiseEither<RequestError, string[]> {
        return ajaxGet(`${ExerciseSettingsController.endpointPath}refTables/backends`, io.array(io.string));
    }

    getDomains() : PromiseEither<RequestError, Domain[]> {
        return ajaxGet(`${ExerciseSettingsController.endpointPath}refTables/domains`, io.array(TDomain));
    }

    getDomainLaws(domainsId: string) : PromiseEither<RequestError, string[]> {
        return ajaxGet(`${ExerciseSettingsController.endpointPath}refTables/domainLaws?domaindId=${encodeURIComponent(domainsId)}`, io.array(io.string));
    }

    getDomainConcepts(domainsId: string) : PromiseEither<RequestError, string[]> {
        return ajaxGet(`${ExerciseSettingsController.endpointPath}refTables/domainConcepts?domaindId=${encodeURIComponent(domainsId)}`, io.array(io.string));
    }
}