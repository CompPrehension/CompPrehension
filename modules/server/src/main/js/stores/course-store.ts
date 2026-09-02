import { makeAutoObservable } from 'mobx';
import * as E from 'fp-ts/lib/Either';
import { courseController } from '../controllers';
import { ExerciseListItem, ExerciseListPermissions, noExerciseListPermissions } from '../types/exercise-settings';
import { RequestError } from '../types/request-error';

export class CourseStore {
    courseId: number | null = null;
    exercises: ExerciseListItem[] = [];
    permissions: ExerciseListPermissions = noExerciseListPermissions;
    loadStatus: 'NONE' | 'LOADING' | 'LOADED' | 'FAILED' = 'NONE';
    error: RequestError | null = null;

    constructor() {
        makeAutoObservable(this);
    }

    async loadCourse(courseId: number) {
        this.loadStatus = 'LOADING';
        this.courseId = courseId;
        this.error = null;

        const r = await courseController.getCourseExercises(courseId);
        if (E.isLeft(r)) {
            this.error = r.left;
            this.loadStatus = 'FAILED';
            return;
        }

        this.exercises = r.right.exercises;
        this.permissions = r.right.permissions;
        this.loadStatus = 'LOADED';
    }
}
