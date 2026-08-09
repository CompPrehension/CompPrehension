import { injectable } from "tsyringe";
import { Domain, ExerciseCard, ExerciseCardConcept, ExerciseCardLaw, ExerciseCardSkill, ExerciseList, ExerciseListItem, QuestionBankSearchResult, Strategy, TDomain, TExerciseCard, TExerciseList, TExerciseListItem, TQuestionBankSearchResult, TStrategy } from "../../types/exercise-settings";
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

    listExercises(courseId: number | null): PromiseEither<RequestError, ExerciseList> {
        const q = courseId == null ? '' : `?courseId=${courseId}`;
        return ajaxGet(`${API_URL}/api/exercise/list${q}`, TExerciseList);
    }

    getExercise(id: number, courseId: number | null = null): PromiseEither<RequestError, ExerciseCard> {
        const courseQ = courseId == null ? '' : `&courseId=${courseId}`;
        return ajaxGet(`${API_URL}/api/exercise?id=${encodeURIComponent(id)}${courseQ}`, TExerciseCard);
    }

    saveExercise(card: ExerciseCard, courseId: number | null = null): PromiseEither<RequestError, void> {
        const q = courseId == null ? '' : `?courseId=${courseId}`;
        return ajaxPost(`${API_URL}/api/exercise${q}`, toJS(card));
    }

    createExercise(name: string, domainId: string, strategyId: string, courseId: number | null = null): PromiseEither<RequestError, number> {
        return ajaxPut(`${API_URL}/api/exercise`, { name, domainId, strategyId, courseId }, io.number);
    }

    cloneExercise(id: number, courseId: number | null): PromiseEither<RequestError, number> {
        const q = courseId == null ? '' : `?courseId=${courseId}`;
        return ajaxPost(`${API_URL}/api/exercise/${id}/clone${q}`, {}, io.number);
    }

    deleteExercise(id: number, courseId: number | null = null): PromiseEither<RequestError, void> {
        const courseQ = courseId == null ? '' : `&courseId=${courseId}`;
        return ajaxDelete(`${API_URL}/api/exercise?id=${encodeURIComponent(id)}${courseQ}`);
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

    search(domainId: string, concepts: ExerciseCardConcept[], laws: ExerciseCardLaw[], skills: ExerciseCardSkill[], tags: string[], complexity: number, limit: number, signal?: AbortSignal): PromiseEither<RequestError, QuestionBankSearchResult> {
        const body = {
            domainId,
            tags,
            concepts,
            laws,
            skills,
            complexity,
            limit,
        }
        return ajaxPost(`${API_URL}/api/question-bank/search`, body, TQuestionBankSearchResult, signal);
    }
}
