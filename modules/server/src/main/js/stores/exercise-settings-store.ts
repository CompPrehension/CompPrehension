import { action, flow, makeObservable, observable, runInAction, toJS } from "mobx";
import { inject, injectable } from "tsyringe";
import { ExerciseSettingsController } from "../controllers/exercise/exercise-settings";
import { Domain, DomainConceptFlag, ExerciseCard, ExerciseCardConceptKind, ExerciseCardViewModel, ExerciseListItem, ExerciseStage, Strategy } from "../types/exercise-settings";
import * as E from "fp-ts/lib/Either";
import { ExerciseOptions } from "../types/exercise-options";
import { KeysWithValsOfType } from "../types/utils";
import { ExerciseController, IExerciseController } from "../controllers/exercise/exercise-controller";
import { UserInfo } from "../types/user-info";
import { Language } from "../types/language";
import i18next from "i18next";
import { NonEmptyArray } from "fp-ts/lib/NonEmptyArray";


@injectable()
export class ExerciseSettingsStore {
    @observable exercisesLoadStatus: 'NONE' | 'LOADING' | 'LOADED' | 'EXERCISELOADING' = 'NONE';
    @observable exercises: ExerciseListItem[] | null = null;
    @observable domains: Domain[] | null = null;
    @observable backends: string[] | null = null;
    @observable strategies: Strategy[] | null = null;
    @observable currentCard: ExerciseCardViewModel | null = null;
    @observable user: UserInfo | null = null;

    constructor(
        @inject(ExerciseSettingsController) private readonly exerciseSettingsController: ExerciseSettingsController,
        @inject(ExerciseController) private readonly exerciseController: IExerciseController) {
        
        makeObservable(this);
    }

    private toCardViewModel(card: ExerciseCard): ExerciseCardViewModel {
        const cardDomain = this.domains?.find(x => x.id === card.domainId);
        if (!cardDomain)
            throw new Error(`не найден домен ${card.domainId}`);    

        return {
            ...card,
            tags: card.tags.join(', '),
            stages: card.stages
                .map(stage => ({
                    ...stage,
                    concepts: stage.concepts.filter(c => cardDomain.concepts.some(cc => cc.name === c.name)),
                    laws: stage.laws.filter(l => cardDomain.laws.some(ll => ll.name === l.name)),
                })) as NonEmptyArray<ExerciseStage>,
        }
    }

    private fromCardViewModel(card: ExerciseCardViewModel): ExerciseCard {
        return {
            ...card,
            tags: card.tags.split(',')
                .map(t => t.trim()),
        }
    }


    async loadExercises() {
        if (this.exercisesLoadStatus === 'LOADED' || this.exercisesLoadStatus === 'LOADING')
            return;

        runInAction(() => this.exercisesLoadStatus = 'LOADING');
        const [rawExercises, domains, backends, strategies, user] = await Promise.all([
            this.exerciseSettingsController.getAllExercises(),
            this.exerciseSettingsController.getDomains(),
            this.exerciseSettingsController.getBackends(),
            this.exerciseSettingsController.getStrategies(),
            this.exerciseController.getCurrentUser(),
        ])
        if (E.isRight(rawExercises) && E.isRight(domains) &&
            E.isRight(backends) && E.isRight(strategies) && E.isRight(user)) {
            runInAction(() => {
                this.exercises = rawExercises.right;
                this.domains = domains.right;
                this.backends = backends.right;
                this.strategies = strategies.right;
                this.user = user.right;
                i18next.changeLanguage(user.right.language);
            });
        }
        runInAction(() => this.exercisesLoadStatus = 'LOADED');
    }

    @action
    changeLanguage = (newLang: Language) => {
        if (this.user && this.user.language !== newLang) {
            this.user.language = newLang;
            i18next.changeLanguage(newLang);
        }
    }

    async loadExercise(exerciseId: number) {
        if (this.exercisesLoadStatus !== 'LOADED')
            throw new Error("Exercises must be loaded first");

        runInAction(() => this.exercisesLoadStatus = 'EXERCISELOADING');
        const rawExercise = await this.exerciseSettingsController.getExercise(exerciseId);
        if (E.isRight(rawExercise)) {
            runInAction(() => {
                this.currentCard = this.toCardViewModel(rawExercise.right);
            });
        }
        runInAction(() => this.exercisesLoadStatus = 'LOADED');
    }

    async createNewExecise() {
        if (this.exercisesLoadStatus !== 'LOADED')
            throw new Error("Exercises must be loaded first");

        const newExerciseId = await this.exerciseSettingsController.createExercise("(empty)", this.domains![0].id, this.strategies![0]!.id);
        if (!E.isRight(newExerciseId))
            return;

        runInAction(() => this.exercisesLoadStatus = 'EXERCISELOADING');
        const [rawExercise, newExercisesList] = await Promise.all([
            this.exerciseSettingsController.getExercise(newExerciseId.right),
            this.exerciseSettingsController.getAllExercises(),
        ]);
        if (E.isRight(rawExercise) && E.isRight(newExercisesList)) {
            runInAction(() => {
                this.currentCard = this.toCardViewModel(rawExercise.right);
                this.exercises = newExercisesList.right;
            });
        }
        runInAction(() => this.exercisesLoadStatus = 'LOADED');
    }


    async saveCard() {
        if (!this.currentCard)
            return;

        runInAction(() => this.exercisesLoadStatus = 'EXERCISELOADING');
        await this.exerciseSettingsController.saveExercise(this.fromCardViewModel(this.currentCard));
        const newExercisesList = await this.exerciseSettingsController.getAllExercises();
        if (E.isRight(newExercisesList)) {
            runInAction(() => {
                this.exercises = newExercisesList.right;
            })
        }
        runInAction(() => this.exercisesLoadStatus = 'LOADED');
    }

    @action
    setCardName(name: string) {
        if (!this.currentCard)
            return;
        this.currentCard.name = name;
    }
    @action
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
    @action
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
    @action
    setCardQuestionComplexity(rawComplexity: string) {
        if (!this.currentCard)
            return;

        const complexity = Number.parseInt(rawComplexity);
        this.currentCard.complexity = complexity / 100.0;
    }
    
    @action
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
                stage.concepts.push(targetConcept);
            }
            targetConcept.kind = conceptValue;
        }
    }
    @action
    setCardStageConceptValue(stageIdx: number, conceptName: string, conceptValue: ExerciseCardConceptKind) {
        if (!this.currentCard || !this.currentCard.stages[stageIdx])
            return;
        const stage = this.currentCard.stages[stageIdx];const targetConceptIdx = stage.concepts.findIndex(x => x.name == conceptName);
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
            stage.concepts.push(targetConcept);
        }
        targetConcept.kind = conceptValue;
    }
    @action
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
                stage.laws.push(targetLaw);
            }
            targetLaw.kind = lawValue;
        }        
    }
    @action
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
            stage.laws.push(targetLaw);
        }
        targetLaw.kind = lawValue;
    }
    @action
    setCardStageNumberOfQuestions(stageIdx: number, rawNumberOfQuesions: string) {
        if (!this.currentCard || !this.currentCard.stages[stageIdx])
            return;
        const stage = this.currentCard.stages[stageIdx];
        if (!rawNumberOfQuesions.match(/^\d*$/))
            return;
        const numb = +rawNumberOfQuesions || 1;
        stage.numberOfQuestions = numb;
    }
    @action
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
    @action
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
    @action
    setCardTags(tags: string) {
        if (!this.currentCard)
            return;
        this.currentCard.tags = tags;
    }

    @action
    setCardFlag(optionId: KeysWithValsOfType<ExerciseOptions, boolean>, checked: boolean) {
        if (!this.currentCard)
            return;
        this.currentCard.options[optionId] = checked;
    }
    @action
    addStage() {
        if (!this.currentCard || !this.domains)
            return;

        const card = this.currentCard;

        const sharedDomainLaws = this.domains.find(z => z.id === card.domainId)?.laws
            .filter(l => (l.bitflags & DomainConceptFlag.TargetEnabled) === 0) ?? [];
        const sharedDomainConcepts = this.domains.find(z => z.id === card.domainId)?.concepts
            .flatMap(c => [c, ...c.childs])
            .filter(c => (c.bitflags & DomainConceptFlag.TargetEnabled) === 0) ?? [];
        var stageConcepts = card.stages[0].concepts
            .filter(c => c.kind !== 'PERMITTED' && sharedDomainConcepts.some(x => x.name === c.name))
        var stageLaws = card.stages[0].laws
            .filter(l => l.kind !== 'PERMITTED' && sharedDomainLaws.some(x => x.name === l.name));

        this.currentCard.stages.push({
            numberOfQuestions: 10,
            laws: stageLaws,
            concepts: stageConcepts,
        });
    }
    @action
    removeStage(stageIdx: number) {
        if (!this.currentCard)
            return;
        const length = this.currentCard.stages.length;
        if (stageIdx < 0 || stageIdx >= length)
            return;

        this.currentCard.stages.splice(stageIdx, 1);
    }
}