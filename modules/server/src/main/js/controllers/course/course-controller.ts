import { injectable } from 'tsyringe';
import * as io from 'io-ts';
import { ajaxDelete, ajaxGet, ajaxPost, PromiseEither } from '../../utils/ajax';
import { API_URL } from '../../appconfig';
import { CourseDto, TCourseDto } from '../../types/course';
import { ExerciseListItem, TExerciseListItem } from '../../types/exercise-settings';
import { RequestError } from '../../types/request-error';

@injectable()
export class CourseController {
    getMyCourses(): PromiseEither<RequestError, CourseDto[]> {
        return ajaxGet(`${API_URL}/api/course/my`, io.array(TCourseDto));
    }

    getGlobalPool(): PromiseEither<RequestError, ExerciseListItem[]> {
        return ajaxGet(`${API_URL}/api/exercise/global-pool`, io.array(TExerciseListItem));
    }

    getCourseExercises(courseId: number): PromiseEither<RequestError, ExerciseListItem[]> {
        return ajaxGet(`${API_URL}/api/exercise/list?courseId=${courseId}`, io.array(TExerciseListItem));
    }

    getExerciseMemberships(exerciseId: number): PromiseEither<RequestError, CourseDto[]> {
        return ajaxGet(`${API_URL}/api/course/memberships?exerciseId=${exerciseId}`, io.array(TCourseDto));
    }

    addExerciseToCourse(exerciseId: number, courseId: number): PromiseEither<RequestError, void> {
        return ajaxPost(`${API_URL}/api/course/exercise/add?exerciseId=${exerciseId}&courseId=${courseId}`, {});
    }

    removeExerciseFromCourse(exerciseId: number, courseId: number): PromiseEither<RequestError, void> {
        return ajaxDelete(`${API_URL}/api/course/exercise/remove?exerciseId=${exerciseId}&courseId=${courseId}`);
    }
}
