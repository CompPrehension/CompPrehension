import React, { useCallback, useEffect, useState } from "react";
import { container } from "tsyringe";
import { ExerciseSettingsController } from "../controllers/exercise/exercise-settings";
import * as E from "fp-ts/lib/Either";
import { ExerciseListItem } from "../types/exercise-settings";
import { ExerciseSettingsStore } from "../stores/exercise-settings-store";
import { observer } from "mobx-react";

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
                <div className="col-xl-2 col-md-3 col-12 d-flex flex-column">
                    <ul className="list-group">
                        {exerciseStore.exercises?.map(e => 
                            <a href="#" className="list-group-item">
                                {e.name}
                            </a>)}
                    </ul>
                </div>
                <div className="col-xl-8 col-md-9 col-12">
                    
                </div>
            </div>
        </div>
    );
})

