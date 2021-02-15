import * as React from 'react';
import store from '../../store';
import { observer } from 'mobx-react';
import { OrderQuestion } from "./question-types/order-question";

export const QuestionFabric = observer(() => {
    const questionData = store.questionData;
    if (!questionData) {
        return null;
    }

    switch (true) {        
        case questionData.type == "ORDER" && questionData.options.requireContext:
            return <OrderQuestion />;
    }

    return (<div>Unsupported question type</div>);
});
