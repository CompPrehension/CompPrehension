import { observer } from "mobx-react";
import React from "react";
import store from '../../../store';
import Select, { components } from 'react-select';


export const MatchingQuestion : React.FC = observer(() => {
    const { questionData } = store;
    if (!questionData || questionData.type != "MATCHING") {
        return null;
    }

    const { options } = questionData;
    switch(options.displayMode) {
        case "combobox":
            return <ComboboxMatchingQuestion />;
        case "dragNdrop":
            return <DragAndDropMatchingQuestion />;
        default:
            return <div>NonImplemented</div>;    
    }
});


const DragAndDropMatchingQuestion = observer(() => {
    const { questionData, answersHistory, onAnswersChanged } = store;
    if (!questionData || questionData.type != "MATCHING") {
        return null;
    }
    const { answers = [], groups = [] } = questionData;

   
    return (<div>NonImplemented</div>);
});


const RawHtmlSelectOption = (props : any) => {
    const {innerRef, innerProps, children, ...rest} = props;
    return (   
        <components.Option {...rest}>
            <div ref={innerRef} {...innerProps} dangerouslySetInnerHTML={{__html: props.label}}></div>
        </components.Option>
    );
};

const RawHtmlSelectSingleValue = (props : any) => {
    const { innerRef, innerProps, children, ...rest } = props;
    return (   
        <components.SingleValue {...rest}>
            {props.getValue()?.map((v : any) => <div ref={innerRef} {...innerProps} dangerouslySetInnerHTML={{__html: v.label}}></div>)}
        </components.SingleValue>
    );
};

const ComboboxMatchingQuestion = observer(() => {
    const { questionData, answersHistory, onAnswersChanged } = store;
    if (!questionData || questionData.type != "MATCHING") {
        return null;
    }

    const { answers = [], groups = [], options } = questionData;
        
    let currentState : Record<number, number> = {};  
    if (answersHistory.length) {
        currentState = answersHistory.reduce((acc, [l, r]) => (acc[l] = r, acc), {} as Record<number, number>);
    }

    return (
        <div>
            <p dangerouslySetInnerHTML={{ __html: questionData.text }}>                
            </p>
            <table style={{ minWidth: '50%' }}>
                <tbody>
                    {answers.map(asw => 
                        <tr>
                            <td dangerouslySetInnerHTML={{ __html: asw.text}}></td>
                            <td>
                                <Select defaultValue={(currentState?.[asw.id] ?? null) as any}
                                        options={groups//.filter(g => !options.hideSelected || !Object.values(currentState).includes(g.id) || currentState[asw.id] == g.id)
                                                       .map(g => ({ value: g.id, label: g.text }))}
                                        components={{ Option: RawHtmlSelectOption, SingleValue: RawHtmlSelectSingleValue }}               
                                        onChange={(v => {
                                            if (!v) {
                                               return;
                                            }

                                            const state = currentState;
                                            state[asw.id] = v.value;
                                            store.updateAnswersHistory(Object.keys(state).map(k => [+k, +state[+k]]));
                                        })} />                                
                            </td>
                        </tr>
                    )}
                </tbody>
            </table>
            
        </div>
    );
});
