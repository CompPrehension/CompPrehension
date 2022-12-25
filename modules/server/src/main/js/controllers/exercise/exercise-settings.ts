import { injectable } from "tsyringe";
import { Domain, ExerciseCard, ExerciseListItem, Strategy, TDomain, TExerciseCard, TExerciseListItem, TStrategy } from "../../types/exercise-settings";
import { ajaxDelete, ajaxGet, ajaxPost, ajaxPut, PromiseEither } from "../../utils/ajax";
import * as io from 'io-ts';
import { RequestError } from "../../types/request-error";
import { API_URL } from "../../appconfig";
import { toJS } from "mobx";
import { TOptionalRequestResult, TOptionalRequestResultV } from "../../utils/helpers";


@injectable()
export class ExerciseSettingsController {

    getAllExercises(): PromiseEither<RequestError, ExerciseListItem[]> {
        return ajaxGet(`/api/exercise/all`, io.array(TExerciseListItem));
    }

    getExercise(id: number): PromiseEither<RequestError, ExerciseCard> {
        return ajaxGet(`/api/exercise?id=${encodeURIComponent(id)}`, TExerciseCard);
    }

    saveExercise(card: ExerciseCard): PromiseEither<RequestError, void> {
        return ajaxPost(`/api/exercise`, toJS(card));
    }

    createExercise(name: string, domainId: string, strategyId: string): PromiseEither<RequestError, number> {
        return ajaxPut(`/api/exercise`, { name, domainId, strategyId }, io.number);
    }

    deleteExercise(id: number): PromiseEither<RequestError, void> {
        return ajaxDelete(`/api/exercise?id=${encodeURIComponent(id)}`);
    }

    getStrategies() : PromiseEither<RequestError, Strategy[]> {
        return ajaxGet(`/api/refTables/strategies`, io.array(TStrategy));
    }

    getBackends() : PromiseEither<RequestError, string[]> {
        return ajaxGet(`/api/refTables/backends`, io.array(io.string));
    }

    getDomains() : PromiseEither<RequestError, Domain[]> {
        return ajaxGet(`/api/refTables/domains`, io.array(TDomain));
    }

    getDomainLaws(domainsId: string) : PromiseEither<RequestError, string[]> {
        return ajaxGet(`/api/refTables/domainLaws?domaindId=${encodeURIComponent(domainsId)}`, io.array(io.string));
    }

    getDomainConcepts(domainsId: string) : PromiseEither<RequestError, string[]> {
        return ajaxGet(`/api/refTables/domainConcepts?domaindId=${encodeURIComponent(domainsId)}`, io.array(io.string));
    }
}