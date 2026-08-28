import { injectable } from 'tsyringe';
import * as io from 'io-ts';
import { ajaxDelete, ajaxGet, ajaxPost, PromiseEither } from '../../utils/ajax';
import { API_URL } from '../../appconfig';
import { CourseDto, TCourseDto } from '../../types/course';
import { ExerciseList, TExerciseList } from '../../types/exercise-settings';
import { RequestError } from '../../types/request-error';

@injectable()
export class CourseController {
    getMyCourses(): PromiseEither<RequestError, CourseDto[]> {
        return ajaxGet(`${API_URL}/api/course/my`, io.array(TCourseDto));
    }

    getCourseExercises(courseId: number): PromiseEither<RequestError, ExerciseList> {
        return ajaxGet(`${API_URL}/api/exercise/list?courseId=${courseId}`, TExerciseList);
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
