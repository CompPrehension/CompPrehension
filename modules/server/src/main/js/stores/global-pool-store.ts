import { makeAutoObservable, runInAction } from 'mobx';
import { inject, injectable } from 'tsyringe';
import * as E from 'fp-ts/lib/Either';
import { ExerciseSettingsController } from '../controllers/exercise/exercise-settings';
import { CourseController } from '../controllers/course/course-controller';
import { ExerciseListItem } from '../types/exercise-settings';

export type ImportMode = 'INHERIT' | 'CLONE';

@injectable()
export class GlobalPoolStore {
    exercises: ExerciseListItem[] = [];
    loadStatus: 'NONE' | 'LOADING' | 'LOADED' = 'NONE';

    constructor(
        @inject(ExerciseSettingsController) private readonly settingsController: ExerciseSettingsController,
        @inject(CourseController) private readonly courseController: CourseController,
    ) {
        makeAutoObservable(this);
    }

    async loadGlobalPool() {
        runInAction(() => { this.loadStatus = 'LOADING'; });
        const r = await this.courseController.getGlobalPool();
        if (E.isRight(r)) {
            runInAction(() => { this.exercises = r.right; });
        }
        runInAction(() => { this.loadStatus = 'LOADED'; });
    }

    async importToCourse(exerciseId: number, targetCourseId: number, mode: ImportMode): Promise<boolean> {
        if (mode === 'INHERIT') {
            const r = await this.courseController.addExerciseToCourse(exerciseId, targetCourseId);
            return E.isRight(r);
        }
        const r = await this.settingsController.cloneExercise(exerciseId, targetCourseId);
        return E.isRight(r);
    }
}
