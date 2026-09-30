import parse, { DOMNode, Element, HTMLReactParserOptions, attributesToProps, domToReact } from "html-react-parser";
import { observer } from "mobx-react";
import React from "react";
import { Dropdown } from "react-bootstrap";
import { Answer } from "../../../types/answer";
import { Feedback } from "../../../types/feedback";
import { MatchingQuestion } from "../../../types/question";
import { answerSlotId } from "./answer-slot";

const EXPRESSION_PART_CLASS = 'comp-ph-expr-part';

type InlineMatchingQuestionComponentProps = {
    question: MatchingQuestion,
    getAnswers: () => Answer[],
    getFeedback?: () => Feedback | undefined,
    onChanged: (newAnswers: Answer[]) => void,
}

const isExpressionPart = (node: Element) =>
    (node.attribs.class ?? '').split(/\s+/).includes(EXPRESSION_PART_CLASS);

function collectSlotIds(nodes: DOMNode[], into: number[]): number[] {
    for (const node of nodes) {
        const slotId = answerSlotId(node);
        if (slotId !== null) {
            into.push(slotId);
        }
        if (node instanceof Element) {
            collectSlotIds(node.children as DOMNode[], into);
        }
    }
    return into;
}

/**
 * Menus sit right in the question text. A part of an expression holds its own slot and nested parts:
 * its menu opens only after every nested part is answered, and an accepted answer can no longer be changed.
 */
export const InlineMatchingQuestionComponent = observer((props: InlineMatchingQuestionComponentProps) => {
    const { question, getAnswers, getFeedback, onChanged } = props;
    if (question.options.displayMode !== 'inline') {
        return null;
    }
    const { groups = [] } = question;
    const accepted = new Set((getFeedback?.()?.correctAnswers ?? []).map(a => a.answer[0]));

    const choose = (slotId: number, groupId: number) => {
        const otherAnswers = getAnswers().filter(a => a.answer[0] !== slotId);
        onChanged([...otherAnswers, { answer: [slotId, groupId], isCreatedByUser: true }]);
    };

    const renderSlot = (slot: Element, slotId: number, disabled: boolean) => {
        const chosen = groups.find(g => g.id === getAnswers().find(a => a.answer[0] === slotId)?.answer[1]);
        return (
            // The id keeps the wrapper's "freezed/finished" styles working for the slot.
            <Dropdown key={`slot-${slotId}`} id={slot.attribs.id} drop="end" className="comp-ph-expr-slot">
                <Dropdown.Toggle as="button" type="button" disabled={disabled}>
                    {chosen ? <span dangerouslySetInnerHTML={{ __html: chosen.text }} /> : '...'}
                </Dropdown.Toggle>
                <Dropdown.Menu popperConfig={{ strategy: 'fixed' }}>
                    {groups.map(g =>
                        <Dropdown.Item key={g.id} as="button" type="button" onClick={() => choose(slotId, g.id)}>
                            <span dangerouslySetInnerHTML={{ __html: g.text }} />
                        </Dropdown.Item>
                    )}
                </Dropdown.Menu>
            </Dropdown>
        );
    };

    const parserOptions: HTMLReactParserOptions = {
        replace: (node) => {
            if (!(node instanceof Element)) {
                return;
            }
            const slotId = answerSlotId(node);
            if (slotId !== null) {
                return renderSlot(node, slotId, accepted.has(slotId));
            }
            if (!isExpressionPart(node)) {
                return;
            }
            const children = node.children as DOMNode[];
            const ownSlot = children.find(child => answerSlotId(child) !== null) as Element | undefined;
            const ownSlotId = ownSlot ? answerSlotId(ownSlot) : null;
            const nestedSlotIds = collectSlotIds(children, []).filter(id => id !== ownSlotId);
            const solved = ownSlotId !== null && accepted.has(ownSlotId);
            const blocked = nestedSlotIds.some(id => !accepted.has(id));
            return (
                <span {...attributesToProps(node.attribs)} className={`${node.attribs.class}${solved ? ' solved' : ''}`}>
                    {children.map((child, index) => child === ownSlot && ownSlotId !== null
                        ? renderSlot(ownSlot, ownSlotId, solved || blocked)
                        : <React.Fragment key={index}>{domToReact([child], parserOptions)}</React.Fragment>)}
                </span>
            );
        },
    };

    return (
        <div id={`question_${question.questionId}`}>
            <div className="comp-ph-question-text">{parse(question.text, parserOptions)}</div>
        </div>
    );
});
