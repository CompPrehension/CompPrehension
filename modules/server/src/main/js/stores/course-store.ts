import { makeAutoObservable, runInAction } from 'mobx';
import { inject, injectable } from 'tsyringe';
import * as E from 'fp-ts/lib/Either';
import { CourseController } from '../controllers/course/course-controller';
import { ExerciseListItem, ExerciseListPermissions, noExerciseListPermissions } from '../types/exercise-settings';
import { RequestError } from '../types/request-error';

@injectable()
export class CourseStore {
    courseId: number | null = null;
    exercises: ExerciseListItem[] = [];
    permissions: ExerciseListPermissions = noExerciseListPermissions;
    loadStatus: 'NONE' | 'LOADING' | 'LOADED' | 'FAILED' = 'NONE';
    error: RequestError | null = null;

    constructor(
        @inject(CourseController) private readonly courseController: CourseController,
    ) {
        makeAutoObservable(this);
    }

    async loadCourse(courseId: number) {
        runInAction(() => {
            this.loadStatus = 'LOADING';
            this.courseId = courseId;
            this.error = null;
        });
        const r = await this.courseController.getCourseExercises(courseId);
        runInAction(() => {
            if (E.isLeft(r)) {
                this.error = r.left;
                this.loadStatus = 'FAILED';
                return;
            }
            this.exercises = r.right.exercises;
            this.permissions = r.right.permissions;
            this.loadStatus = 'LOADED';
        });
    }
}
