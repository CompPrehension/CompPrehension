import { observer } from 'mobx-react';
import * as React from 'react';
import store from '../store';
import { Pagination as BootstrapPagination } from "react-bootstrap"

export const Pagination = observer(() => {
    if (!(store.sessionInfo?.questionIds.length)) {
        return null;
    }

    return (
        <div className="d-flex justify-content-center">
            <BootstrapPagination className="p-3" style={{ marginBottom: '0 !important' }}>
                {/*<BootstrapPagination.First />*/}
                {/*<BootstrapPagination.Prev />*/}
                {store.sessionInfo.questionIds.map((id, idx) => 
                    (<BootstrapPagination.Item key={idx + 1} 
                                               active={store.questionData?.questionId === id}
                                               onClick={() => store.loadQuestion(id)}>
                        {idx + 1}
                     </BootstrapPagination.Item>))}
                {/*<BootstrapPagination.Next />*/}
                {/*<BootstrapPagination.Last />*/}
            </BootstrapPagination>
        </div>
    );
})
