import { observer } from 'mobx-react';
import * as React from 'react';
import { useState } from 'react';
import { Pagination as BootstrapPagination } from "react-bootstrap"
import { container } from "tsyringe";
import { ExerciseStore } from "../../stores/exercise-store";

export const Pagination = observer(() => {
    const [exerciseStore] = useState(() => container.resolve(ExerciseStore));
    if (!(exerciseStore.currentAttempt?.questionIds.length) || !exerciseStore.currentQuestion) {
        return null;
    }

    return (
        <div className="d-flex justify-content-center">
            <BootstrapPagination className="p-3" style={{ marginBottom: '0 !important' }}>
                {/*<BootstrapPagination.First />*/}
                {/*<BootstrapPagination.Prev />*/}
                {exerciseStore.currentAttempt?.questionIds.map((id, idx) => 
                    (<BootstrapPagination.Item key={idx + 1} 
                                               active={exerciseStore.currentQuestion.question?.questionId === id}
                                               onClick={() => exerciseStore.currentQuestion.loadQuestion(id)}>
                        {idx + 1}
                     </BootstrapPagination.Item>))}
                {/*<BootstrapPagination.Next />*/}
                {/*<BootstrapPagination.Last />*/}
            </BootstrapPagination>
        </div>
    );
})
