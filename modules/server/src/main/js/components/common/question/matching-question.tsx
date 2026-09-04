import { Droppable, DroppableEventNames, DroppableStopEvent, Plugins } from "@shopify/draggable";
import type { DraggableEventNames } from "@shopify/draggable/lib/draggable.bundle.legacy";
import parse from "html-react-parser";
import { observer } from "mobx-react";
import React, { useEffect } from "react";
import Select, { OptionProps, SingleValueProps, components } from "react-select";
import { Answer } from "../../../types/answer";
import { Feedback } from "../../../types/feedback";
import { MatchingQuestion } from "../../../types/question";
import { answerSlotId } from "./answer-slot";

type GroupOption = { value: number, label: string };

/**
 * The store owns the answers, so the selects are controlled by it: `defaultValue` would
 * only be read once and would ignore everything that does not come from a click - a
 * reloaded question, or a step the server filled in.
 */
const selectedOption = (options: GroupOption[], answers: Answer[], slotId: number) =>
    options.find(o => o.value === answers.find(a => a.answer[0] === slotId)?.answer[1]) ?? null;

type MatchingQuestionComponentProps = {
    question: MatchingQuestion,
    answers: Answer[],
    getAnswers: () => Answer[],
    getFeedback?: () => Feedback | undefined,
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
    const { question, getAnswers, getFeedback, onChanged } = props;
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
                Object.assign(e.style, dropzoneStyle);
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

        droppable.on('droppable:stop', (e: DroppableStopEvent) => {
            const draggableId: string | undefined = e.dragEvent?.source?.id;
            const droppableId: string | undefined = e.dropzone?.id;
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
                        const slot = e.parentElement;
                        const leftId = slot?.getAttribute('data-answer-id')
                            ?? slot?.id.split(`question_${question.questionId}_answer_`)[1]
                            ?? '';
                        const rightId = e?.id.split('dragAnswer_')[1] ?? '';
                        return [+leftId, +rightId];
                    });
                const oldHistory = getAnswers();
                
                onChanged(newHistory.map(h => 
                    ({ answer: h, isCreatedByUser: oldHistory.find(x => x.answer[0] === h[0] && x.answer[1] === h[1])?.isCreatedByUser ?? true })));
            }, 10);
        });
        // the drag library is set up once per question: re-running it on every render
        // would tear down a live drag session
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [question.questionId])

    const answerKey = getAnswers().map(a => a.answer.join(':')).join(',');
    const confirmed = new Set((getFeedback?.()?.correctAnswers ?? []).map(a => a.answer.join(':')));
    const confirmedKey = [...confirmed].join(',');
    useEffect(() => {
        const slots = Array.from(document.querySelectorAll<HTMLElement>(`[id^="question_${question.questionId}_answer_"]`));
        const answers = getAnswers();

        slots.forEach(slot => {
            const slotId = +(slot.getAttribute('data-answer-id')
                ?? slot.id.split(`question_${question.questionId}_answer_`)[1]
                ?? '');
            const answer = answers.find(a => a.answer[0] === slotId);
            const placed = slot.querySelector<HTMLElement>('.comp-ph-draggable');
            const placedGroupId = +(placed?.id.split('dragAnswer_')[1] ?? '');

            if (placed && answer?.answer[1] !== placedGroupId) {
                const wrapper = document.getElementById(`dragAnswerWrapper_${placedGroupId}`);
                if (!options.multipleSelectionEnabled && wrapper && !wrapper.querySelector('.comp-ph-draggable')) {
                    wrapper.appendChild(placed);
                } else {
                    placed.remove();
                }
                slot.classList.remove('draggable-dropzone--occupied');
            }

            if (answer && !slot.querySelector('.comp-ph-draggable')) {
                const source = document.querySelector(`#dragAnswerWrapper_${answer.answer[1]} .comp-ph-draggable`);
                if (source) {
                    slot.appendChild(options.multipleSelectionEnabled ? source.cloneNode(true) : source);
                    slot.classList.add('draggable-dropzone--occupied');
                }
            }

            slot.classList.toggle('comp-ph-answer-locked',
                answer !== undefined && confirmed.has(answer.answer.join(':')));
        });
        // answerKey and confirmedKey stand in for the answers themselves: the callbacks
        // are new on every render, the serialised keys change only when the data does
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [question.questionId, answerKey, confirmedKey])

    return (
        <div>
            {!options.requireContext &&
                <p className="mb-3 comp-ph-question-text" dangerouslySetInnerHTML={{ __html: question.text }} />}
            <div className="row">
                <div className="col-md">                    
                    {
                        !options.requireContext
                            ? <p className="d-flex flex-column comp-ph-droppable-container comp-ph-question-text">
                                {question.answers.map(a =>
                                    <div className="d-flex flex-row mb-3">
                                        <div className="me-2 mt-1">
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
    const groupOptions: GroupOption[] = groups//.filter(g => !options.hideSelected || !Object.values(currentState).includes(g.id) || currentState[asw.id] == g.id)
                                              .map(g => ({ value: g.id, label: g.text }));
    return (
        <div>
            <p className="mb-5 comp-ph-question-text" dangerouslySetInnerHTML={{ __html: question.text }} />            
            <div>
                {question.answers.map(asw => 
                    <div className="row mb-3">
                        <div className="col-md-6" dangerouslySetInnerHTML={{ __html: asw.text}}>
                        </div>
                        <div className="col-md-auto">
                            <div style={{width: `${(8*groupsMaxLength) + 100}px`}}>
                                <Select value={selectedOption(groupOptions, getAnswers(), asw.id)}
                                        options={groupOptions}
                                        components={{ Option: RawHtmlSelectOption, SingleValue: RawHtmlSelectSingleValue }}               
                                        onChange={(v => {
                                            if (!v) {
                                                return;
                                            }
                                            const otherHistoryItems = getAnswers().filter(v => v.answer[0] !== asw.id);
                                            const historyItem = { answer: [asw.id, +v.value] as [number, number], isCreatedByUser: true };
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
    const { groups = [] } = question;
    const groupOptions: GroupOption[] = groups.map(g => ({ value: g.id, label: g.text }));

    const content = parse(question.text, {
        replace: (node) => {
            const answerId = answerSlotId(node);
            if (answerId === null) {
                return;
            }

            return (
                <Select value={selectedOption(groupOptions, getAnswers(), answerId)}
                        options={groupOptions}
                        components={{ Option: RawHtmlSelectOption, SingleValue: RawHtmlSelectSingleValue }}
                        onChange={(v => {
                            if (!v) {
                                return;
                            }

                            const otherHistoryItems = getAnswers().filter(v => v.answer[0] !== answerId);
                            const historyItem = { answer: [answerId, +v.value] as [number, number], isCreatedByUser: true };
                            const newAnswersHistory = [...otherHistoryItems, historyItem];
                            onChanged(newAnswersHistory);
                        })}
                />
            );
        },
    });

    return (
        <div id={`question_${question.questionId}`}>
            <div className="comp-ph-question-text">{content}</div>
        </div>
    );
});

const RawHtmlSelectOption = (props: OptionProps<GroupOption, false>) => (
    <components.Option {...props}>
        <div dangerouslySetInnerHTML={{ __html: props.data.label }}></div>
    </components.Option>
);

const RawHtmlSelectSingleValue = (props: SingleValueProps<GroupOption, false>) => (
    <components.SingleValue {...props}>
        <div dangerouslySetInnerHTML={{ __html: props.data.label }}></div>
    </components.SingleValue>
);
