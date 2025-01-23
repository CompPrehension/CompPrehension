import { injectable } from "tsyringe";
import { Domain, ExerciseCard, ExerciseCardConcept, ExerciseCardLaw, ExerciseCardSkill, ExerciseListItem, QuestionBankSearchResult, Strategy, TDomain, TExerciseCard, TExerciseListItem, TQuestionBankSearchResult, TStrategy } from "../../types/exercise-settings";
import { ajaxDelete, ajaxGet, ajaxPost, ajaxPut, PromiseEither } from "../../utils/ajax";
import * as io from 'io-ts';
import { RequestError } from "../../types/request-error";
import { API_URL } from "../../appconfig";
import { toJS } from "mobx";
import { TOptionalRequestResult, TOptionalRequestResultV, delayPromise, getRandomInt } from "../../utils/helpers";
import * as E from "fp-ts/lib/Either";


@injectable()
export class ExerciseSettingsController {

    getAllExercises(): PromiseEither<RequestError, ExerciseListItem[]> {
        return ajaxGet(`${API_URL}/api/exercise/all`, io.array(TExerciseListItem));
    }

    getExercise(id: number): PromiseEither<RequestError, ExerciseCard> {
        return ajaxGet(`${API_URL}/api/exercise?id=${encodeURIComponent(id)}`, TExerciseCard);
    }

    saveExercise(card: ExerciseCard): PromiseEither<RequestError, void> {
        return ajaxPost(`${API_URL}/api/exercise`, toJS(card));
    }

    createExercise(name: string, domainId: string, strategyId: string): PromiseEither<RequestError, number> {
        return ajaxPut(`${API_URL}/api/exercise`, { name, domainId, strategyId }, io.number);
    }

    deleteExercise(id: number): PromiseEither<RequestError, void> {
        return ajaxDelete(`${API_URL}/api/exercise?id=${encodeURIComponent(id)}`);
    }

    getStrategies() : PromiseEither<RequestError, Strategy[]> {
        return ajaxGet(`${API_URL}/api/refTables/strategies`, io.array(TStrategy));
    }

    getBackends() : PromiseEither<RequestError, string[]> {
        return ajaxGet(`${API_URL}/api/refTables/backends`, io.array(io.string));
    }

    getDomains() : PromiseEither<RequestError, Domain[]> {
        return ajaxGet(`${API_URL}/api/refTables/domains`, io.array(TDomain));
    }

    getDomainLaws(domainsId: string) : PromiseEither<RequestError, string[]> {
        return ajaxGet(`${API_URL}/api/refTables/domainLaws?domaindId=${encodeURIComponent(domainsId)}`, io.array(io.string));
    }

    getDomainConcepts(domainsId: string) : PromiseEither<RequestError, string[]> {
        return ajaxGet(`${API_URL}/api/refTables/domainConcepts?domaindId=${encodeURIComponent(domainsId)}`, io.array(io.string));
    }

    search(domainId: string, concepts: ExerciseCardConcept[], laws: ExerciseCardLaw[], skills: ExerciseCardSkill[], tags: string[], complexity: number, limit: number): PromiseEither<RequestError, QuestionBankSearchResult> {
        const body = {
            domainId,
            tags,
            concepts,
            laws,
            skills,
            complexity,
            limit,
        }
        return ajaxPost(`${API_URL}/api/question-bank/search`, body, TQuestionBankSearchResult);
    }
}
