import { makeAutoObservable, runInAction } from 'mobx';
import { inject, injectable } from 'tsyringe';
import * as E from 'fp-ts/lib/Either';
import { CourseController } from '../controllers/course/course-controller';
import { ExerciseListItem } from '../types/exercise-settings';

@injectable()
export class CourseStore {
    courseId: number | null = null;
    exercises: ExerciseListItem[] = [];
    permissions: string[] = [];
    loadStatus: 'NONE' | 'LOADING' | 'LOADED' = 'NONE';

    constructor(
        @inject(CourseController) private readonly courseController: CourseController,
    ) {
        makeAutoObservable(this);
    }

    async loadCourse(courseId: number) {
        runInAction(() => {
            this.loadStatus = 'LOADING';
            this.courseId = courseId;
        });
        const r = await this.courseController.getCourseExercises(courseId);
        if (E.isRight(r)) {
            runInAction(() => {
                this.exercises = r.right.exercises;
                this.permissions = r.right.permissions;
            });
        }
        runInAction(() => { this.loadStatus = 'LOADED'; });
    }
}
