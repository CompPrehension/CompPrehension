import React, { useCallback, useEffect, useState } from "react";
import { container } from "tsyringe";
import { ExerciseSettingsController } from "../controllers/exercise/exercise-settings";
import * as E from "fp-ts/lib/Either";
import { Domain, ExerciseCard, ExerciseListItem } from "../types/exercise-settings";
import { ExerciseSettingsStore } from "../stores/exercise-settings-store";
import { observer } from "mobx-react";
import { ToggleSwitch } from "../components/common/toggle";

export const ExerciseSettings = observer(() => {
    const [exerciseStore] = useState(() => container.resolve(ExerciseSettingsStore));

    useEffect(() => {
        (async () => {
            await exerciseStore.loadExercises();
        })()
    }, []);

    return (
        <div className="container-fluid">
            <div className="flex-xl-nowrap row">
                <div className="col-xl-3 col-md-3 col-12 d-flex flex-column">
                    <ul className="list-group">
                        {exerciseStore.exercises?.map(e =>
                            <a href="#" className="list-group-item" onClick={() => exerciseStore.loadExercise(e.id)}>
                                {e.name}
                            </a>)}
                        <button style={{marginTop: "10px"}} type="button" className="btn btn-primary">Create new</button>
                    </ul>
                </div>
                <div className="col-xl-9 col-md-9 col-12">
                    <ExerciseCardElement
                        store={exerciseStore}
                        card={exerciseStore.currentCard}
                        domains={exerciseStore.domains ?? []}
                        backends={exerciseStore.backends ?? []}
                        strategies={exerciseStore.strategies ?? []}
                    />
                </div>
            </div>
        </div>
    );
})


type ExerciseCardElementProps = {
    store: ExerciseSettingsStore,
    card?: ExerciseCard | null,
    domains: Domain[],
    backends: string[],
    strategies: string[],
}

const ExerciseCardElement = observer((props: ExerciseCardElementProps) => {
    const { card, domains, backends, strategies, store } = props;

    if (card == null)
        return (<div>No exercise selected</div>);

    return (
        <div>
            <form className="exercise-settings-form">
                <div className="form-group">
                    <label htmlFor="exampleInputEmail1">Name</label>
                    <input value={card.name} type="email" className="form-control" id="exampleInputEmail1" aria-describedby="emailHelp" placeholder="Enter email" onChange={e => store.setCardName(e.target.value)}/>
                </div>
                <div className="form-group">
                    <label className="font-weight-bold">Domain</label>
                    <select className="form-control" value={card.domainId} onChange={e => store.setCardDomain(e.target.value)}>
                        {domains?.map(d => <option>{d.name}</option>)}
                    </select>
                </div>
                <div className="form-group">
                    <label className="font-weight-bold">Strategy</label>
                    <select className="form-control" value={card.strategyId} onChange={e => store.setCardStrategy(e.target.value)}>
                        {strategies?.map(d => <option>{d}</option>)}
                    </select>
                </div>
                <div className="row">
                    <div className="col-md-6">
                        <div className="form-group">
                            <label className="font-weight-bold">Question complexity</label>
                            <div>
                                <input type="range" className="form-control-range" id="formControlRange1" onChange={e => store.setCardQuestionComplexity(e.target.value)}/>
                            </div>
                        </div>
                    </div>
                    <div className="col-md-6">
                        <div className="form-group">
                            <label className="font-weight-bold">Answer length</label>
                            <div>
                                <input type="range" className="form-control-range" id="formControlRange1" onChange={e => store.setCardAnswerLength(e.target.value)}/>
                            </div>
                        </div>
                    </div>
                </div>

                <div className="form-group">
                    <div>
                        <label className="font-weight-bold">Concepts</label>
                    </div>
                    <div className="row">
                        <div className="col-md-12">
                            {domains.find(z => z.id === card.domainId)?.concepts.map((c, idx) =>
                                        <div className="d-flex flex-row align-items-center" style={{ marginBottom: '10px' }}>
                                            <ToggleSwitch id={`concept_toggle_${card.id}_${c.name}_${idx}`}
                                                selected={'Allowed'}
                                                values={['Denied', 'Allowed', 'Target']}
                                                valueStyles={[{ backgroundColor: '#eb2828' }, null, { backgroundColor: '#009700' }]}
                                                onChange={val => 0} />
                                            <div style={{ marginLeft: '15px' }}>{c.displayName}</div>
                            </div>)}
                        </div>
                    </div>
                    {/* <div className="font-weight-bold text-justify" style={{ marginTop: '-10px', fontSize: '19px', letterSpacing: '3px' }}>...</div> */}

                </div>
                <div className="form-group">
                    <label className="font-weight-bold">Laws</label>
                    {domains.find(z => z.id === card.domainId)?.laws.map((c, idx) =>
                            (<div className="d-flex flex-row align-items-start justify-content-start" style={{ marginBottom: '10px' }}>
                                <div>
                                    <ToggleSwitch id={`law_toggle_${card.id}_${idx}`}
                                        selected={'Allowed'}
                                        values={['Denied', 'Allowed', 'Target']}
                                        valueStyles={[{ backgroundColor: '#eb2828' }, null, { backgroundColor: '#009700' }]}
                                        onChange={val => 0} />
                                </div>
                                <div style={{ marginLeft: '15px' }}>{c.name}</div>
                        </div>))}
                    {/* <div className="font-weight-bold text-justify" style={{ marginTop: '-10px', fontSize: '19px', letterSpacing: '3px' }}>...</div> */}
                </div>
            </form>
            <button type="button" className="btn btn-primary" onClick={() => store.saveCard()}>Save</button>
        </div>


    );
})