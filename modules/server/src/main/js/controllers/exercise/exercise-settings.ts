import { injectable } from "tsyringe";
import { ExerciseListItem, TExerciseListItem } from "../../types/exercise-settings";
import { ajaxGet, PromiseEither } from "../../utils/ajax";
import * as io from 'io-ts';
import { RequestError } from "../../types/request-error";


@injectable()
export class ExerciseSettingsController {
    static endpointPath: `/` | `/${string}/` = ExerciseSettingsController.initEndpointPath();
    private static initEndpointPath(): `/` | `/${string}/` {
        const matches = /^\/(?:.+\/(?<!\/pages\/))?/.exec(window.location.pathname)
        if (matches) {
            return <`/${string}/`>matches[0];
        }
        return "/";
    }

    getAll(): PromiseEither<RequestError, ExerciseListItem[]> {
        return ajaxGet(`${ExerciseSettingsController.endpointPath}exercise/all`, io.array(TExerciseListItem));
    }
}