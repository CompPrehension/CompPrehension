import { action, flow, makeObservable, observable, runInAction, toJS } from "mobx";
import { inject, injectable } from "tsyringe";
import { ExerciseSettingsController } from "../controllers/exercise/exercise-settings";
import { Domain, ExerciseCard, ExerciseCardConceptKind, ExerciseCardViewModel, ExerciseListItem } from "../types/exercise-settings";
import * as E from "fp-ts/lib/Either";
import { ExerciseOptions } from "../types/exercise-options";
import { KeysWithValsOfType } from "../types/utils";


@injectable()
export class ExerciseSettingsStore {
    @observable exercisesLoadStatus: 'NONE' | 'LOADING' | 'LOADED' | 'EXERCISELOADING' = 'NONE';
    @observable exercises: ExerciseListItem[] | null = null;
    @observable domains: Domain[] | null = null;
    @observable backends: string[] | null = null;
    @observable strategies: string[] | null = null;
    @observable currentCard: ExerciseCardViewModel | null = null;

    constructor(@inject(ExerciseSettingsController) private readonly exerciseSettingsController: ExerciseSettingsController) {
        makeObservable(this);
    }

    private toCardViewModel(card: ExerciseCard): ExerciseCardViewModel {
        return {
            ...card,
            tags: card.tags.join(', '),
            stages: [
                {
                    numberOfQuestions: 10,
                    laws: card.laws,
                    concepts: card.concepts,
                },
            ],
        }
    }

    private fromCardViewModel(card: ExerciseCardViewModel): ExerciseCard {
        return {
            ...card,
            tags: card.tags.split(',')
                .map(t => t.trim()),
            laws: card.stages[0].laws,
            concepts: card.stages[0].concepts,
        }
    }


    async loadExercises() {
        if (this.exercisesLoadStatus === 'LOADED' || this.exercisesLoadStatus === 'LOADING')
            return;

        runInAction(() => this.exercisesLoadStatus = 'LOADING');
        const [rawExercises, domains, backends, strategies] = await Promise.all([
            this.exerciseSettingsController.getAllExercises(),
            this.exerciseSettingsController.getDomains(),
            this.exerciseSettingsController.getBackends(),
            this.exerciseSettingsController.getStrategies(),
        ])
        if (E.isRight(rawExercises) && E.isRight(domains) &&
            E.isRight(backends) && E.isRight(strategies)) {
            runInAction(() => {
                this.exercises = rawExercises.right;
                this.domains = domains.right;
                this.backends = backends.right;
                this.strategies = strategies.right;
            });
        }
        runInAction(() => this.exercisesLoadStatus = 'LOADED');
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

        const newExerciseId = await this.exerciseSettingsController.createExercise("(empty)", this.domains![0].id, this.strategies![0]);
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
        this.currentCard.strategyId = strategyId;
    }
    @action
    setCardQuestionComplexity(rawComplexity: string) {
        if (!this.currentCard)
            return;

        const complexity = Number.parseInt(rawComplexity);
        this.currentCard.complexity = complexity / 100.0;
    }
    @action
    setCardAnswerLength(rawLength: string) {
        if (!this.currentCard)
            return;
        const length = Number.parseInt(rawLength);
        this.currentCard.answerLength = length / 100.0;
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
        if (!this.currentCard)
            return;
        this.currentCard.stages.push({
            numberOfQuestions: 10,
            laws: [],
            concepts: [],
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