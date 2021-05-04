import { observer } from "mobx-react";
import React, { useEffect, useState } from "react";
import Select, { components } from 'react-select';
import ReactDOM from "react-dom";
import { container } from "tsyringe";
import { ExerciseStore } from "../../../../stores/exercise-store";
// @ts-ignore
import { Droppable, DroppableEventNames } from '@shopify/draggable';


export const MatchingQuestion = observer(() => {
    const [exerciseStore] = useState(() => container.resolve(ExerciseStore));
    const { currentQuestion } = exerciseStore;
    if (!currentQuestion || currentQuestion.type != "MATCHING") {
        return null;
    }

    const { options } = currentQuestion;
    switch(true) {
        case options.displayMode === "combobox" && !options.requireContext:
            return <ComboboxMatchingQuestion />;
        case options.displayMode === "combobox" && options.requireContext:
            return <ComboboxMatchingQuestionWithCtx />;
        case options.displayMode === "dragNdrop":
            return <DragAndDropMatchingQuestion />;
        default:
            return <div>NonImplemented</div>;  
    }
});


const DragAndDropMatchingQuestion = observer(() => {
    const [exerciseStore] = useState(() => container.resolve(ExerciseStore));
    const { currentQuestion, answersHistory, onAnswersChanged } = exerciseStore;
    if (!currentQuestion || currentQuestion.type != "MATCHING") {
        return null;
    }
    const { groups = [] } = currentQuestion;
    const { options } = currentQuestion;


    // on question first render
    const dropzoneStyle = { height: '40px', width: '80px' };
    useEffect(() => {
        document.querySelectorAll('[id^="answer_"]')
            .forEach((e: any) => {
                e.classList.add("comp-ph-dropzone");
                e.style.height = dropzoneStyle.height;
                e.style.width = dropzoneStyle.width;
            });

        const droppable = new Droppable<DroppableEventNames>(document.querySelectorAll('.comp-ph-droppable-container'), {
            draggable: '.comp-ph-draggable',
            dropzone: '.comp-ph-dropzone',
        })

        droppable.on('droppable:stop', (e: any) => {
            const draggableId: string | undefined = e?.data?.dragEvent?.data?.source?.id;
            const droppableId: string | undefined = e?.data?.dropzone?.id;
            if (!draggableId || !droppableId) {
                return;
            }

            // clone draggable element and return it back
            if (options.multipleSelectionEnabled) {
                const wrapperId = `dragAnswerWrapper_${draggableId.split('_')[1] ?? ''}`;
                const wrapper = document.getElementById(wrapperId);
                const draggable = document.getElementById(draggableId);
                if (wrapper && draggable) {
                    wrapper.innerHTML = draggable.outerHTML;
                }
            }

            // setTimeout is needed to guarantee completion of all dnd events
            setTimeout(() => {
                const newHistory = [...(document.querySelectorAll('[id^="answer_"] > [id^="dragAnswer_"]') as unknown as Element[])]
                    .map<[number, number]>(e => {
                        const leftId = e.parentElement?.id.split('_')[1] ?? '';
                        const rightId = e?.id.split('_')[1] ?? '';
                        return [+leftId, +rightId];
                    });                        
                if (exerciseStore.isHistoryChanged(newHistory)) {
                    exerciseStore.updateAnswersHistory(newHistory);
                }                
            }, 10);
        });
    }, [currentQuestion.questionId])
    
    return (
        <div>
            <div className="row comp-ph-droppable-container">
                <div className="col-md">
                    <p dangerouslySetInnerHTML={{ __html: currentQuestion.text }} />
                    <p className="d-flex flex-column">
                        {currentQuestion.answers.map(a =>
                            <div className="d-flex flex-row mb-3">
                                <div className="mr-2 mt-1">
                                    <div id={`answer_${a.id}`} className="comp-ph-dropzone" style={dropzoneStyle}></div>
                                </div>
                                <div>{a.text}</div>
                            </div>)}
                    </p>                    
                </div>
                <div className="col-md">
                    {groups.map(g => 
                        (<div id={`dragAnswerWrapper_${g.id}`} className="comp-ph-dropzone draggable-dropzone--occupied d-flex flex-column mb-2" style={dropzoneStyle}>
                            <div id={`dragAnswer_${g.id}`} className="comp-ph-draggable p-2 d-flex justify-content-center align-items-center" dangerouslySetInnerHTML={{ __html: g.text }}/>
                         </div>))}
                </div>
            </div>
        </div>);
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

const ComboboxMatchingQuestionWithCtx = observer(() => {
    const [exerciseStore] = useState(() => container.resolve(ExerciseStore));
    const { currentQuestion, answersHistory, onAnswersChanged } = exerciseStore;
    if (!currentQuestion || currentQuestion.type != "MATCHING" || !currentQuestion.options.requireContext) {
        return null;
    }
    const { groups = [], options } = currentQuestion;      

    useEffect(() => {
        // replace all placeholders on first render
        document.querySelectorAll('[id^="answer_"]')
            .forEach(elem => {
                const answerId = +elem.id?.split("answer_")[1];
                const selector = <Select options={groups.map(g => ({ value: g.id, label: g.text }))}
                                         components={{ Option: RawHtmlSelectOption, SingleValue: RawHtmlSelectSingleValue }} 
                                         onChange={(v => {
                                            if (!v) {
                                               return;
                                            }

                                            const otherHistoryItems = answersHistory.filter(v => v[0] !== answerId);
                                            const historyItem = [answerId, +v.value] as [number, number];
                                            const newAnswersHistory = [...otherHistoryItems, historyItem];
                                            exerciseStore.updateAnswersHistory(newAnswersHistory);     
                                        })}
                                    />
                ReactDOM.render(selector, elem);
            });        
    }, [currentQuestion.questionId]);

    return (
        <div>
            <p dangerouslySetInnerHTML={{ __html: currentQuestion.text }} />
        </div>
    );
});

const ComboboxMatchingQuestion = observer(() => {
    const [exerciseStore] = useState(() => container.resolve(ExerciseStore));
    const { currentQuestion, answersHistory, onAnswersChanged } = exerciseStore;
    if (!currentQuestion || currentQuestion.type != "MATCHING" || currentQuestion.options.requireContext) {
        return null;
    }

    const { answers = [], groups = [], options } = currentQuestion;   

    return (
        <div>
            <p dangerouslySetInnerHTML={{ __html: currentQuestion.text }}>                
            </p>
            <table style={{ minWidth: '50%' }}>
                <tbody>
                    {answers.map(asw => 
                        <tr>
                            <td dangerouslySetInnerHTML={{ __html: asw.text}}></td>
                            <td>
                                <Select defaultValue={(answersHistory.find(v => v[0] === asw.id)?.[1] ?? null) as any}
                                        options={groups//.filter(g => !options.hideSelected || !Object.values(currentState).includes(g.id) || currentState[asw.id] == g.id)
                                                       .map(g => ({ value: g.id, label: g.text }))}
                                        components={{ Option: RawHtmlSelectOption, SingleValue: RawHtmlSelectSingleValue }}               
                                        onChange={(v => {
                                            if (!v) {
                                               return;
                                            }
                                            const otherHistoryItems = answersHistory.filter(v => v[0] !== asw.id);
                                            const historyItem = [asw.id, +v.value] as [number, number];
                                            const newAnswersHistory = [...otherHistoryItems, historyItem];
                                            exerciseStore.updateAnswersHistory(newAnswersHistory);                                            
                                        })} />                                
                            </td>
                        </tr>
                    )}
                </tbody>
            </table>
            
        </div>
    );
});
