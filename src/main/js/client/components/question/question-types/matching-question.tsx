import { observer } from "mobx-react";
import React from "react";
import store from '../../../store';
import Select from 'react-select';


export const MatchingQuestion : React.FC = observer(() => {
    const { questionData, answersHistory, onAnswersChanged } = store;
    if (!questionData || questionData.type != "MATCHING") {
        return null;
    }

    const { answers = [], groups = [] } = questionData;  
    
    let currentState : Record<string, any> | null = null;  
    if (answersHistory.length) {
        currentState = JSON.parse(answersHistory[answersHistory.length - 1]) as unknown as Record<string, any>;
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
                                <Select defaultValue={currentState?.[asw.id] ?? null}
                                        options={groups.map(g => ({ value: g.id, label: g.text }))}
                                        onChange={(v => {
                                            if (v !== null) {
                                                const state = currentState ?? {}
                                                state[asw.id] = v.value;
                                                store.onAnswersChanged(JSON.stringify(state));
                                            }
                                        })} />                                
                            </td>
                        </tr>
                    )}
                </tbody>
            </table>
            
        </div>
    );
})
