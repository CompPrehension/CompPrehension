import { makeAutoObservable, runInAction } from 'mobx';
import { inject, injectable } from 'tsyringe';
import * as E from 'fp-ts/lib/Either';
import { ExerciseSettingsController } from '../controllers/exercise/exercise-settings';
import { ExerciseListItem } from '../types/exercise-settings';

@injectable()
export class CourseStore {
    courseId: number | null = null;
    exercises: ExerciseListItem[] = [];
    loadStatus: 'NONE' | 'LOADING' | 'LOADED' = 'NONE';

    constructor(
        @inject(ExerciseSettingsController) private readonly settingsController: ExerciseSettingsController,
    ) {
        makeAutoObservable(this);
    }

    async loadCourse(courseId: number) {
        runInAction(() => {
            this.loadStatus = 'LOADING';
            this.courseId = courseId;
        });
        const r = await this.settingsController.listExercises(courseId);
        if (E.isRight(r)) {
            runInAction(() => { this.exercises = r.right; });
        }
        runInAction(() => { this.loadStatus = 'LOADED'; });
    }
}
