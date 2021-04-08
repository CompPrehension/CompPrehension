import { observer } from 'mobx-react';
import * as React from 'react';
import { exerciseStore } from "../stores/exercise-store";
import { Pagination as BootstrapPagination } from "react-bootstrap"

export const Pagination = observer(() => {
    if (!(exerciseStore.currentAttempt?.questionIds.length)) {
        return null;
    }

    return (
        <div className="d-flex justify-content-center">
            <BootstrapPagination className="p-3" style={{ marginBottom: '0 !important' }}>
                {/*<BootstrapPagination.First />*/}
                {/*<BootstrapPagination.Prev />*/}
                {exerciseStore.currentAttempt?.questionIds.map((id, idx) => 
                    (<BootstrapPagination.Item key={idx + 1} 
                                               active={exerciseStore.currentQuestion?.questionId === id}
                                               onClick={() => exerciseStore.loadQuestion(id)}>
                        {idx + 1}
                     </BootstrapPagination.Item>))}
                {/*<BootstrapPagination.Next />*/}
                {/*<BootstrapPagination.Last />*/}
            </BootstrapPagination>
        </div>
    );
})
