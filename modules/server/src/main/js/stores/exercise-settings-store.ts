import { action, autorun, comparer, flow, makeAutoObservable, makeObservable, observable, reaction, runInAction, toJS, when } from "mobx";
import { inject, injectable } from "tsyringe";
import { ExerciseSettingsController } from "../controllers/exercise/exercise-settings";
import { Domain, DomainConceptFlag, ExerciseCard, ExerciseCardConcept, ExerciseCardConceptKind, ExerciseCardLaw, ExerciseListItem, ExerciseStage, Strategy } from "../types/exercise-settings";
import * as E from "fp-ts/lib/Either";
import { ExerciseOptions } from "../types/exercise-options";
import { KeysWithValsOfType } from "../types/utils";
import { ExerciseController, IExerciseController } from "../controllers/exercise/exercise-controller";
import { UserInfo } from "../types/user-info";
import { Language } from "../types/language";
import i18next from "i18next";
import * as NEA from "fp-ts/lib/NonEmptyArray";
import { pipe } from "fp-ts/lib/function";
import { RequestError } from "../types/request-error";

export type ExerciseCardViewModel = {
    id: number,
    name: string,
    domainId: string,
    strategyId: string,
    backendId: string,
    tags: string[],
    complexity: number;
    stages: NEA.NonEmptyArray<ExerciseStageStore>,
    options: ExerciseOptions,
}

export class ExerciseStageStore {
    card: ExerciseCardViewModel
    concepts: ExerciseCardConcept[]
    laws: ExerciseCardLaw[]
    numberOfQuestions: number
    bankLoadingState: 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED' = 'NOT_STARTED'
    bankQuestionsCount: number = 0

    constructor(private readonly exerciseSettingsController: ExerciseSettingsController, card: ExerciseCardViewModel, stage: ExerciseStage) {
        this.concepts = stage.concepts;
        this.laws     = stage.laws;
        this.numberOfQuestions = stage.numberOfQuestions;
        this.card     = card;

        makeAutoObservable(this);

        autorun(async () => {
            const complexity = this.card.complexity;
            const laws = this.laws.slice()
            const concepts = this.concepts.slice()
            await this.updateBankStats(concepts, laws, card.tags, complexity);
        }, { delay: 1000 });
    }
    
    *updateBankStats(concepts: ExerciseCardConcept[], laws: ExerciseCardLaw[], tags: string[], complexity: number) {
        const { card } = this;
        runInAction(() => this.bankLoadingState = 'IN_PROGRESS');
        const newData: E.Either<RequestError, number> = yield this.exerciseSettingsController.getBankStats(card.domainId, concepts, laws, tags, complexity);
        if (E.isRight(newData)) {
            runInAction(() => {
                this.bankQuestionsCount = newData.right;
            });
        }
        runInAction(() => this.bankLoadingState = 'COMPLETED');
    }
}

@injectable()
export class ExerciseSettingsStore {
    exercisesLoadStatus: 'NONE' | 'LOADING' | 'LOADED' | 'EXERCISELOADING' = 'NONE';
    exercises: ExerciseListItem[] | null = null;
    domains: Domain[] | null = null;
    backends: string[] | null = null;
    strategies: Strategy[] | null = null;
    currentCard: ExerciseCardViewModel | null = null;
    user: UserInfo | null = null;

    constructor(
        @inject(ExerciseSettingsController) private readonly exerciseSettingsController: ExerciseSettingsController,
        @inject(ExerciseController) private readonly exerciseController: IExerciseController) {
        
        makeAutoObservable(this);
    }

    private toCardViewModel(card: ExerciseCard): ExerciseCardViewModel {
        const cardDomain = this.domains?.find(x => x.id === card.domainId);
        if (!cardDomain)
            throw new Error(`не найден домен ${card.domainId}`);    

        const result: ExerciseCardViewModel = observable({
            ...card,
            tags: card.tags.filter(t => cardDomain.tags.some(tt => tt === t)),
            stages: [] as any,
        });
        result.stages = pipe(
            card.stages,
            NEA.map(stage => new ExerciseStageStore(this.exerciseSettingsController, result, stage))
        );

        return result;
    }

    private fromCardViewModel(card: ExerciseCardViewModel): ExerciseCard {
        return {
            ...card,
            stages: pipe(
                card.stages,
                NEA.map(stage => ({ concepts: stage.concepts, laws: stage.laws, numberOfQuestions: stage.numberOfQuestions }))
            ),
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
    
    setCardQuestionComplexity(rawComplexity: string) {
        if (!this.currentCard)
            return;

        const complexity = Number.parseInt(rawComplexity);
        this.currentCard.complexity = complexity / 100.0;
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

    
    setCardFlag(optionId: KeysWithValsOfType<ExerciseOptions, boolean>, checked: boolean) {
        if (!this.currentCard)
            return;
        this.currentCard.options[optionId] = checked;
    }
    
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

        const newStage = new ExerciseStageStore(
            this.exerciseSettingsController,
            this.currentCard, 
            {
                numberOfQuestions: 10,
                laws: stageLaws,
                concepts: stageConcepts,
            });
        this.currentCard.stages.push(newStage);
    }
    
    
    removeStage(stageIdx: number) {
        if (!this.currentCard)
            return;
        const length = this.currentCard.stages.length;
        if (stageIdx < 0 || stageIdx >= length)
            return;

        this.currentCard.stages.splice(stageIdx, 1);
    }
}
