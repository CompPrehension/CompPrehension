import { Droppable, DroppableEventNames, Plugins } from "@shopify/draggable";
import { DraggableEventNames } from "@shopify/draggable/lib/draggable.bundle.legacy";
import { observer } from "mobx-react";
import React, { useEffect } from "react";
import ReactDOM from "react-dom";
import Select, { components } from "react-select";
import { Answer } from "../../../types/answer";
import { MatchingQuestion } from "../../../types/question";
import { Optional } from "../optional";

type MatchingQuestionComponentProps = {
    question: MatchingQuestion,
    answers: Answer[],
    getAnswers: () => Answer[],
    onChanged: (newAnswers: Answer[]) => void,
}

export const MatchingQuestionComponent = observer((props: MatchingQuestionComponentProps) => {    
    const { question } = props;
    const { options } = question;
    switch(true) {
        case options.displayMode === "combobox" && !options.requireContext:
            return <ComboboxMatchingQuestionComponent {...props}/>;
        case options.displayMode === "combobox" && options.requireContext:
            return <ComboboxMatchingQuestionWithCtxComponent {...props}/>;
        case options.displayMode === "dragNdrop":
            return <DragAndDropMatchingQuestionComponent {...props}/>;              
    }
    return (<div>Not Implemented</div>);
});

export const DragAndDropMatchingQuestionComponent = observer((props: MatchingQuestionComponentProps) => {
    const { question, getAnswers, onChanged } = props;
    if (question.options.displayMode !== 'dragNdrop') {
        return null;
    }
    const { groups = [] } = question;
    const { options } = question;

    // on question first render
    const dropzoneStyle = options.dropzoneStyle && JSON.parse(options.dropzoneStyle) || {};
    const draggableStyle = options.dropzoneStyle && JSON.parse(options.draggableStyle) || {};
    useEffect(() => {
        (document.querySelectorAll(`[id^="question_${question.questionId}_answer_"]`) as unknown as HTMLSpanElement[])
            .forEach(e => {
                e.classList.add("comp-ph-dropzone");
                Object.keys(dropzoneStyle).forEach(k => e.style[k as any] = dropzoneStyle[k]);
                e.innerHTML = `<div class="comp-ph-dropzone-placeholder">${options.dropzoneHtml}</div>`;
            });

        const droppable = new Droppable<DroppableEventNames | DraggableEventNames>(document.querySelectorAll('.comp-ph-droppable-container'), {
            draggable: '.comp-ph-draggable',
            dropzone: '.comp-ph-dropzone',
            plugins: [Plugins.ResizeMirror],
            mirror: {
                constrainDimensions: true,
            },
        })

        droppable.on('drag:over', () => console.log('is out'));

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
                if (wrapperId !== droppableId && wrapper && draggable) {
                    wrapper.innerHTML = draggable.outerHTML;
                }
            }

            // setTimeout is needed to guarantee completion of all dnd events
            setTimeout(() => {
                const newHistory = [...(document.querySelectorAll(`[id^="question_${question.questionId}_answer_"] > [id^="dragAnswer_"]`) as unknown as Element[])]
                    .map<[number, number]>(e => {
                        const leftId = e.parentElement?.id.split(`question_${question.questionId}_answer_`)[1] ?? '';
                        const rightId = e?.id.split('dragAnswer_')[1] ?? '';
                        return [+leftId, +rightId];
                    });
                const oldHistory = getAnswers();
                
                onChanged(newHistory.map(h => 
                    ({ answer: h, isСreatedByUser: oldHistory.find(x => x.answer[0] === h[0] && x.answer[1] === h[1])?.isСreatedByUser ?? true })));
            }, 10);
        });
    }, [question.questionId])
    
    return (
        <div>
            <div className="row">
                <div className="col-md">                    
                    {
                        !options.requireContext
                            ? <p className="d-flex flex-column comp-ph-droppable-container comp-ph-question-text">
                                {question.answers.map(a =>
                                    <div className="d-flex flex-row mb-3">
                                        <div className="mr-2 mt-1">
                                            <div id={`question_${question.questionId}_answer_${a.id}`}></div>
                                        </div>
                                        <div dangerouslySetInnerHTML={{ __html: a.text}}></div>
                                    </div>)}
                            </p>
                            : <p className="comp-ph-droppable-container comp-ph-question-text" dangerouslySetInnerHTML={{ __html: question.text }} />
                    }
                </div>
                <div className="col-md comp-ph-droppable-container d-flex justify-content-start align-items-start flex-column">
                    {groups.map(g => 
                        (<div id={`dragAnswerWrapper_${g.id}`} className="comp-ph-dropzone mb-2" style={dropzoneStyle}>
                            <div className="comp-ph-dropzone-placeholder" dangerouslySetInnerHTML={{ __html: options.dropzoneHtml }}></div>
                            <div id={`dragAnswer_${g.id}`} className="comp-ph-draggable" style={draggableStyle} dangerouslySetInnerHTML={{ __html: g.text }}/>
                         </div>))}
                    
                </div>
            </div>
        </div>);
});

const ComboboxMatchingQuestionComponent = observer((props: MatchingQuestionComponentProps) => {
    const { question, getAnswers, onChanged } = props;
    if (question.options.displayMode !== 'combobox') {
        return null;
    }

    const { groups = [], } = question;   
    const groupsMaxLength = groups.reduce((len, g) => g.text.length > len ? g.text.length : len, 0);
    return (
        <div>
            <p className="mb-5 comp-ph-question-text" dangerouslySetInnerHTML={{ __html: question.text }} />            
            <div>
                {question.answers.map(asw => 
                    <div className="row mb-3">
                        <div className="col-md-6" dangerouslySetInnerHTML={{ __html: question.text}}>
                        </div>
                        <div className="col-md-auto">
                            <div style={{width: `${(8*groupsMaxLength) + 100}px`}}>
                                <Select defaultValue={(getAnswers().find(v => v.answer[0] === asw.id)?.answer?.[1] ?? null) as any}
                                        options={groups//.filter(g => !options.hideSelected || !Object.values(currentState).includes(g.id) || currentState[asw.id] == g.id)
                                                        .map(g => ({ value: g.id, label: g.text }))}
                                        components={{ Option: RawHtmlSelectOption, SingleValue: RawHtmlSelectSingleValue }}               
                                        onChange={(v => {
                                            if (!v) {
                                                return;
                                            }
                                            const otherHistoryItems = getAnswers().filter(v => v.answer[0] !== asw.id);
                                            const historyItem = { answer: [asw.id, +v.value] as [number, number], isСreatedByUser: true };
                                            const newAnswersHistory = [...otherHistoryItems, historyItem];
                                            onChanged(newAnswersHistory);                                            
                                        })} /> 
                            </div>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
});

const ComboboxMatchingQuestionWithCtxComponent = observer((props: MatchingQuestionComponentProps) => {
    const { question, getAnswers, onChanged } = props;
    if (question.options.displayMode !== 'combobox') {
        return null;
    }
    const { groups = [], options } = question;      

    useEffect(() => {
        // replace all placeholders on first render
        document.querySelectorAll(`[id^="question_${question.questionId}_answer_"]`)
            .forEach(elem => {
                const answerId = +elem.id?.split(`question_${question.questionId}_answer_`)[1];
                const selector = <Select options={groups.map(g => ({ value: g.id, label: g.text }))}
                                         components={{ Option: RawHtmlSelectOption, SingleValue: RawHtmlSelectSingleValue }} 
                                         onChange={(v => {
                                            if (!v) {
                                               return;
                                            }

                                            const otherHistoryItems = getAnswers().filter(v => v.answer[0] !== answerId);
                                            const historyItem = { answer: [answerId, +v.value] as [number, number], isСreatedByUser: true };
                                            const newAnswersHistory = [...otherHistoryItems, historyItem];
                                            onChanged(newAnswersHistory);     
                                        })}
                                    />
                ReactDOM.render(selector, elem);
            });        
    }, [question.questionId]);

    return (
        <div>
            <p className="comp-ph-question-text" dangerouslySetInnerHTML={{ __html: question.text }} />
        </div>
    );
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
