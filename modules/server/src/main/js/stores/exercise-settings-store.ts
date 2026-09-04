import { IReactionDisposer, autorun, makeAutoObservable, observable, untracked } from "mobx";
import { courseController, exerciseSettingsController } from "../controllers";
import { Domain, ExerciseCard, ExerciseCardConcept, ExerciseCardConceptKind, ExerciseCardLaw, ExerciseCardPermissions, ExerciseCardSkill, ExerciseList, ExerciseListItem, ExerciseListPermissions, ExerciseStage, QuestionBankSearchResult, Strategy, noExerciseListPermissions } from "../types/exercise-settings";
import * as E from "fp-ts/lib/Either";
import { ExerciseOptions } from "../types/exercise-options";
import { RequestError } from "../types/request-error";
import * as NEA from "fp-ts/lib/NonEmptyArray";
import { pipe } from "fp-ts/lib/function";
import {NonEmptyArray} from "fp-ts/lib/NonEmptyArray";

export type ExerciseCardViewModel = {
    id: number,
    name: string,
    domainId: string,
    strategyId: string,
    backendId: string,
    tags: string[],
    stages: NEA.NonEmptyArray<ExerciseStageStore>,
    options: ExerciseOptions,
    isPublic: boolean,
    permissions: ExerciseCardPermissions,
}

export type ExerciseLinkType = 'global' | 'original' | 'inherited' | 'cloned';

export class ExerciseStageStore implements Disposable {
    card: ExerciseCardViewModel
    concepts: ExerciseCardConcept[]
    laws: ExerciseCardLaw[]
    skills: ExerciseCardSkill[]
    numberOfQuestions: number
    bankLoadingState: 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED' = 'NOT_STARTED'
    bankSearchResult: QuestionBankSearchResult = { questions: [], count: 0, topRatedCount: 0 }
    complexity: number = 0.5
    autorunner?: IReactionDisposer
    private abortController: AbortController | null = null

    constructor(private readonly courseId: number | null,
                card: ExerciseCardViewModel, stage: ExerciseStage) {
        this.concepts = stage.concepts;
        this.laws     = stage.laws;
        this.skills     = stage.skills;
        this.numberOfQuestions = stage.numberOfQuestions;
        this.complexity        = stage.complexity;
        this.card     = card;

        makeAutoObservable(this);

        this.autorunner = autorun(() => {
            const complexity = this.complexity;
            const laws = this.laws.slice()
            const concepts = this.concepts.slice()
            const skills = this.skills.slice()
            const tags = card.tags.slice()
            // the search itself is a side effect, not an input: `makeAutoObservable` marks
            // plain methods as `autoAction`, which keeps tracking when called from inside a
            // derivation, so without `untracked` this autorun would also subscribe to what
            // updateBankStats reads - including the abortController it writes to itself
            untracked(() => this.updateBankStats(concepts, laws, skills, tags, complexity));
        }, { delay: 1000 });
    }
    
    async updateBankStats(concepts: ExerciseCardConcept[], laws: ExerciseCardLaw[], skills: ExerciseCardSkill[], tags: string[], complexity: number) {
        const { card } = this;

        // Cancel previous request
        if (this.abortController) {
            this.abortController.abort();
            this.abortController = null;
        }

        // Create new controller for this request
        const currentAbortController = new AbortController();
        this.abortController = currentAbortController;
        this.bankLoadingState = 'IN_PROGRESS';

        const newData = await exerciseSettingsController.search(card.domainId, concepts, laws, skills, tags, complexity, 5, this.courseId, currentAbortController.signal);
        if (E.isRight(newData)) {
            this.bankSearchResult = newData.right;

            // TODO handle AbortError properly
            this.bankLoadingState = 'COMPLETED';
        }

        // Cleanup if this is still the active request
        if (this.abortController === currentAbortController) {
            this.abortController = null;
        }
    }
    
    [Symbol.dispose](): void {
        if (this.autorunner)
            this.autorunner();
        // Cancel any pending request on dispose
        if (this.abortController) {
            this.abortController.abort();
            this.abortController = null;
        }
    }
}

export class ExerciseSettingsStore {
    exercisesLoadStatus: 'NONE' | 'LOADING' | 'LOADED' | 'EXERCISELOADING' = 'NONE';
    exercises: ExerciseListItem[] | null = null;
    permissions: ExerciseListPermissions = noExerciseListPermissions;
    domains: Domain[] | null = null;
    backends: string[] | null = null;
    strategies: Strategy[] | null = null;
    currentCard: ExerciseCardViewModel | null = null;
    courseId: number | null = null;
    storeState: { tag: 'VALID' } | { tag: 'ERROR', error: RequestError } = { tag: 'VALID' };
    private loadToken = 0;

    constructor() {
        makeAutoObservable(this);
    }

    private applyExerciseList(list: ExerciseList) {
        this.exercises = list.exercises;
        this.permissions = list.permissions;
    }

    get cardLinkType(): ExerciseLinkType {
        const card = this.currentCard;
        if (!card) return 'global';
        if (this.courseId == null) return card.isPublic ? 'global' : 'original';
        return card.isPublic ? 'inherited' : 'original';
    }

    private toCardViewModel(card: ExerciseCard): ExerciseCardViewModel {
        const cardDomain = this.domains?.find(x => x.id === card.domainId);
        if (!cardDomain)
            throw new Error(`The exercise is bound to domain '${card.domainId}', which the server does not list`);

        const result: ExerciseCardViewModel = observable({
            ...card,
            tags: card.tags.filter(t => cardDomain.tags.some(tt => tt === t)),
            stages: [] as unknown as NonEmptyArray<ExerciseStageStore>,
        });
        result.stages = pipe(
            card.stages,
            NEA.map(stage => new ExerciseStageStore(this.courseId, result, stage))
        );

        return result;
    }

    private fromCardViewModel(card: ExerciseCardViewModel): ExerciseCard {
        return {
            ...card,
            stages: pipe(
                card.stages,
                NEA.map(stage => ({ concepts: stage.concepts, laws: stage.laws, skills: stage.skills, numberOfQuestions: stage.numberOfQuestions, complexity: stage.complexity })),
            ),
        }
    }


    async loadExercises(courseId: number | null = null) {
        if (this.exercisesLoadStatus === 'LOADED' || this.exercisesLoadStatus === 'LOADING')
            return;

        this.storeState = { tag: 'VALID' };
        this.exercisesLoadStatus = 'LOADING';
        this.courseId = courseId;
        const [rawExercises, domains, backends, strategies] = await Promise.all([
            exerciseSettingsController.listExercises(courseId),
            exerciseSettingsController.getDomains(),
            exerciseSettingsController.getBackends(),
            exerciseSettingsController.getStrategies()
        ])
        const failed = [rawExercises, domains, backends, strategies].find(E.isLeft);
        if (failed) {
            this.storeState = { tag: 'ERROR', error: failed.left };
            this.exercisesLoadStatus = 'NONE';
            return;
        }

        this.applyExerciseList((rawExercises as E.Right<ExerciseList>).right);
        this.domains = (domains as E.Right<Domain[]>).right;
        this.backends = (backends as E.Right<string[]>).right;
        this.strategies = (strategies as E.Right<Strategy[]>).right;
        this.exercisesLoadStatus = 'LOADED';
    }

    async loadExercise(exerciseId: number) {
        if (this.exercisesLoadStatus === 'NONE' || this.exercisesLoadStatus === 'LOADING')
            throw new Error("Exercises must be loaded first");

        const token = ++this.loadToken;
        this.storeState = { tag: 'VALID' };
        this.exercisesLoadStatus = 'EXERCISELOADING';
        try {
            const rawExercise = await exerciseSettingsController.getExercise(exerciseId, this.courseId);
            if (token !== this.loadToken)
                return;
            if (E.isLeft(rawExercise)) {
                this.storeState = { tag: 'ERROR', error: rawExercise.left };
                return;
            }
            this.currentCard = this.toCardViewModel(rawExercise.right);
        } catch (error) {
            this.currentCard = null;
            this.storeState = { tag: 'ERROR', error: { message: error instanceof Error ? error.message : String(error) } };
        } finally {
            if (token === this.loadToken)
                this.exercisesLoadStatus = 'LOADED';
        }
    }

    async createNewExecise() {
        if (this.exercisesLoadStatus !== 'LOADED')
            throw new Error("Exercises must be loaded first");

        const newExerciseId = await exerciseSettingsController.createExercise(
            "(empty)", this.domains![0].id, this.strategies![0]!.id, this.courseId);
        if (!E.isRight(newExerciseId))
            return;

        this.exercisesLoadStatus = 'EXERCISELOADING';
        const [rawExercise, newExercisesList] = await Promise.all([
            exerciseSettingsController.getExercise(newExerciseId.right, this.courseId),
            exerciseSettingsController.listExercises(this.courseId),
        ]);
        if (E.isRight(rawExercise) && E.isRight(newExercisesList)) {
            this.currentCard = this.toCardViewModel(rawExercise.right);
            this.applyExerciseList(newExercisesList.right);
        }
        this.exercisesLoadStatus = 'LOADED';
    }

    async cloneCurrentToCourse(targetCourseId: number) {
        if (!this.currentCard) return;
        const result = await exerciseSettingsController.cloneExercise(this.currentCard.id, targetCourseId);
        if (!E.isRight(result)) return;
        const newId = result.right;
        // Reload list and load the new clone
        const [rawExercise, newExercisesList] = await Promise.all([
            exerciseSettingsController.getExercise(newId, this.courseId),
            exerciseSettingsController.listExercises(this.courseId),
        ]);
        if (E.isRight(rawExercise) && E.isRight(newExercisesList)) {
            this.currentCard = this.toCardViewModel(rawExercise.right);
            this.applyExerciseList(newExercisesList.right);
        }
    }

    async copyCurrentToPool(): Promise<number | null> {
        if (!this.currentCard) return null;
        const result = await exerciseSettingsController.cloneExercise(this.currentCard.id, null);
        return E.isRight(result) ? result.right : null;
    }

    async unlinkFromCourse(courseId: number) {
        if (!this.currentCard) return;
        await courseController.removeExerciseFromCourse(this.currentCard.id, courseId);
        // After unlink the exercise no longer belongs to this course; reload list and clear card.
        const refreshed = await exerciseSettingsController.listExercises(this.courseId);
        if (E.isRight(refreshed)) {
            this.applyExerciseList(refreshed.right);
            this.currentCard = null;
        }
    }

    async deleteCurrentExercise() {
        if (!this.currentCard) return;
        const id = this.currentCard.id;
        await exerciseSettingsController.deleteExercise(id, this.courseId);
        const refreshed = await exerciseSettingsController.listExercises(this.courseId);
        if (E.isRight(refreshed)) {
            this.applyExerciseList(refreshed.right);
            this.currentCard = null;
        }
    }


    async saveCard() {
        if (!this.currentCard)
            return;

        this.exercisesLoadStatus = 'EXERCISELOADING';
        await exerciseSettingsController.saveExercise(this.fromCardViewModel(this.currentCard), this.courseId);
        const newExercisesList = await exerciseSettingsController.listExercises(this.courseId);
        if (E.isRight(newExercisesList)) {
            this.applyExerciseList(newExercisesList.right);
        }
        this.exercisesLoadStatus = 'LOADED';
    }

    setCardName(name: string) {
        if (!this.currentCard)
            return;
        this.currentCard.name = name;
    }
    
    setCardDomain(domainId: string) {
        if (!this.currentCard)
            return;
        if (domainId !== this.currentCard.domainId) {
            this.currentCard.stages[0].laws = [];
            this.currentCard.stages[0].concepts = [];
            this.currentCard.stages.splice(1);
            this.currentCard.domainId = domainId;
        }

    }
    
    setCardStrategy(strategyId: string) {
        if (!this.currentCard)
            return;
        if (this.currentCard.strategyId !== strategyId) {
            this.currentCard.stages[0].laws = [];
            this.currentCard.stages[0].concepts = [];
            this.currentCard.stages.splice(1);
            this.currentCard.strategyId = strategyId;
        }
        
    }
    
    setCardStageComplexity(stageIdx: number, rawComplexity: string) {
        if (!this.currentCard || !this.currentCard.stages[stageIdx])
            return;

        const stage = this.currentCard.stages[stageIdx];
        const complexity = Number.parseInt(rawComplexity);
        stage.complexity = complexity / 100.0;
    }
    
    
    setCardCommonConceptValue(conceptName: string, conceptValue: ExerciseCardConceptKind) {
        if (!this.currentCard)
            return;
        for(const stage of this.currentCard.stages) {
            const targetConceptIdx = stage.concepts.findIndex(x => x.name == conceptName);
            let targetConcept = targetConceptIdx !== -1 ? stage.concepts[targetConceptIdx] : null;
            if (conceptValue === 'PERMITTED') {
                if (targetConcept)
                    stage.concepts.splice(targetConceptIdx, 1)
                continue;
            }
            if (!targetConcept) {
                targetConcept = {
                    name: conceptName,
                    kind: conceptValue,
                }
                stage.concepts = [...stage.concepts, targetConcept];
            } else {
                stage.concepts[targetConceptIdx] = {
                    ...targetConcept,
                    kind: conceptValue
                }
            }
        }
    }
    
    setCardStageConceptValue(stageIdx: number, conceptName: string, conceptValue: ExerciseCardConceptKind) {
        if (!this.currentCard || !this.currentCard.stages[stageIdx])
            return;

        const stage = this.currentCard.stages[stageIdx];
        const targetConceptIdx = stage.concepts.findIndex(x => x.name == conceptName);
        let targetConcept = targetConceptIdx !== -1 ? stage.concepts[targetConceptIdx] : null;
        if (conceptValue === 'PERMITTED') {
            if (targetConcept)
                stage.concepts.splice(targetConceptIdx, 1)
            return;
        }
        if (!targetConcept) {
            targetConcept = {
                name: conceptName,
                kind: conceptValue,
            }
            stage.concepts = [...stage.concepts, targetConcept];
        } else {
            stage.concepts[targetConceptIdx] = {
                ...targetConcept,
                kind: conceptValue
            }
        }
    }
    
    setCardCommonLawValue(lawName: string, lawValue: ExerciseCardConceptKind) {
        if (!this.currentCard)
            return;
        for(const stage of this.currentCard.stages) {
            const targetLawIdx = stage.laws.findIndex(x => x.name == lawName);
            let targetLaw = targetLawIdx !== -1 ? stage.laws[targetLawIdx] : null;
            if (lawValue === 'PERMITTED') {
                if (targetLaw)
                    stage.laws.splice(targetLawIdx, 1)
                continue;
            }
            if (!targetLaw) {
                targetLaw = {
                    name: lawName,
                    kind: lawValue,
                }
                stage.laws = [...stage.laws, targetLaw];
            } else {
                stage.laws[targetLawIdx] = {
                    ...targetLaw,
                    kind: lawValue,
                }
            }
        }        
    }
    
    setCardStageLawValue(stageIdx: number, lawName: string, lawValue: ExerciseCardConceptKind) {
        if (!this.currentCard || !this.currentCard.stages[stageIdx])
            return;
        const stage = this.currentCard.stages[stageIdx];
        const targetLawIdx = stage.laws.findIndex(x => x.name == lawName);
        let targetLaw = targetLawIdx !== -1 ? stage.laws[targetLawIdx] : null;
        if (lawValue === 'PERMITTED') {
            if (targetLaw)
                stage.laws.splice(targetLawIdx, 1)
            return;
        }
        if (!targetLaw) {
            targetLaw = {
                name: lawName,
                kind: lawValue,
            }
            stage.laws = [...stage.laws, targetLaw];
        } else {
            stage.laws[targetLawIdx] = {
                ...targetLaw,
                kind: lawValue,
            }
        }
    }

     setCardStageSkillValue(stageIdx: number, skillName: string, skillValue: ExerciseCardConceptKind) {
        if (!this.currentCard || !this.currentCard.stages[stageIdx])
            return;
        const stage = this.currentCard.stages[stageIdx];
        const targetSkillIdx = stage.skills.findIndex(x => x.name == skillName);
        let targetSkill = targetSkillIdx !== -1 ? stage.skills[targetSkillIdx] : null;
        if (skillValue === 'PERMITTED') {
            if (targetSkill)
                stage.skills.splice(targetSkillIdx, 1)
            return;
        }
        if (!targetSkill) {
            targetSkill = {
                name: skillName,
                kind: skillValue,
            }
            stage.skills = [...stage.skills, targetSkill];
        } else {
            stage.skills[targetSkillIdx] = {
                ...targetSkill,
                kind: skillValue,
            }
        }
    }

    setCardCommonSkillValue(skillName: string, skillValue: ExerciseCardConceptKind) {
        if (!this.currentCard)
            return;
        for(const stage of this.currentCard.stages) {
            const targetSkillIdx = stage.skills.findIndex(x => x.name == skillName);
            let targetSkill = targetSkillIdx !== -1 ? stage.laws[targetSkillIdx] : null;
            if (skillValue === 'PERMITTED') {
                if (targetSkill)
                    stage.laws.splice(targetSkillIdx, 1)
                continue;
            }
            if (!targetSkill) {
                targetSkill = {
                    name: skillName,
                    kind: skillValue,
                }
                stage.skills = [...stage.skills, targetSkill];
            } else {
                stage.skills[targetSkillIdx] = {
                    ...targetSkill,
                    kind: skillValue,
                }
            }
        }        
    }
    
    setCardStageNumberOfQuestions(stageIdx: number, rawNumberOfQuesions: string) {
        if (!this.currentCard || !this.currentCard.stages[stageIdx])
            return;
        const stage = this.currentCard.stages[stageIdx];
        if (!rawNumberOfQuesions.match(/^\d*$/))
            return;
        const numb = +rawNumberOfQuesions || 1;
        stage.numberOfQuestions = numb;
    }
    
    setCardSurveyEnabled(enabled: boolean) {
        if (!this.currentCard)
            return;
        if (!this.currentCard.options.surveyOptions) {
            this.currentCard.options.surveyOptions = {
                enabled,
                surveyId: '',
            }
            return;
        }
        this.currentCard.options.surveyOptions.enabled = enabled;
    }
    
    setCardSurveyId(surveyId: string) {
        if (!this.currentCard)
            return;
        if (!this.currentCard.options.surveyOptions) {
            this.currentCard.options.surveyOptions = {
                enabled: true,
                surveyId: surveyId,
            }
            return;
        }
        this.currentCard.options.surveyOptions.surveyId = surveyId;
    }
    
    
    setCardTags(tags: string[]) {
        if (!this.currentCard)
            return;
        this.currentCard.tags = tags;
    }

    setCardOption<TKey extends keyof ExerciseOptions, TValue extends ExerciseOptions[TKey]>(optionId: TKey, value: TValue) {
        if (!this.currentCard)
            return;
        this.currentCard.options[optionId] = value;
    }
    
    addStage() {
        if (!this.currentCard || !this.domains)
            return;

        const card = this.currentCard;

        /*
        const sharedDomainLaws = this.domains.find(z => z.id === card.domainId)?.laws
            .filter(l => (l.bitflags & DomainConceptFlag.TargetEnabled) === 0) ?? [];
        const sharedDomainConcepts = this.domains.find(z => z.id === card.domainId)?.concepts
            .flatMap(c => [c, ...c.childs])
            .filter(c => (c.bitflags & DomainConceptFlag.TargetEnabled) === 0) ?? [];
        const sharedDomainSkills = this.domains.find(z => z.id === card.domainId)?.skills
            .flatMap(c => [c, ...c.childs]) ?? [];
        var stageConcepts = card.stages[0].concepts
            .filter(c => c.kind !== 'PERMITTED' && sharedDomainConcepts.some(x => x.name === c.name))
        var stageSkills = card.stages[0].skills
            .filter(c => c.kind !== 'PERMITTED' && sharedDomainSkills.some(x => x.name === c.name))
        var stageLaws = card.stages[0].laws
            .filter(l => l.kind !== 'PERMITTED' && sharedDomainLaws.some(x => x.name === l.name));
        */

        const newStage = new ExerciseStageStore(
            this.courseId,
            card,
            {
                numberOfQuestions: 10,
                complexity: 0.5,
                laws: [],
                concepts: [],
                skills: [],
            });
        this.currentCard.stages.push(newStage);
    }
    
    
    removeStage(stageIdx: number) {
        if (!this.currentCard)
            return;
        const length = this.currentCard.stages.length;
        if (stageIdx < 0 || stageIdx >= length)
            return;

        const stageToRemove = this.currentCard.stages[stageIdx];
        stageToRemove[Symbol.dispose]();

        this.currentCard.stages.splice(stageIdx, 1);
    }
}
