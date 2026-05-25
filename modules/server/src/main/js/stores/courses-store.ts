import { makeAutoObservable, runInAction } from 'mobx';
import { inject, injectable } from 'tsyringe';
import * as E from 'fp-ts/lib/Either';
import { CourseController } from '../controllers/course/course-controller';
import { CourseDto } from '../types/course';

@injectable()
export class CoursesStore {
    courses: CourseDto[] = [];
    loadStatus: 'NONE' | 'LOADING' | 'LOADED' = 'NONE';

    constructor(
        @inject(CourseController) private readonly api: CourseController,
    ) {
        makeAutoObservable(this);
    }

    async loadMyCourses() {
        runInAction(() => { this.loadStatus = 'LOADING'; });
        const r = await this.api.getMyCourses();
        if (E.isRight(r)) {
            runInAction(() => { this.courses = r.right; });
        }
        runInAction(() => { this.loadStatus = 'LOADED'; });
    }
}
