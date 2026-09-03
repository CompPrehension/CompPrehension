import { DOMNode, Element } from "html-react-parser";

/**
 * The backend sends question text as html, and `QuestionStore.onQuestionLoaded` marks the
 * answer positions in it with `data-answer-id`. Returns that id for a slot node, or null
 * for anything else.
 */
export function answerSlotId(node: DOMNode): number | null {
    const attribs = (node as Element).attribs;
    if (node.type !== 'tag' || attribs?.['data-answer-id'] === undefined) {
        return null;
    }
    return +attribs['data-answer-id'];
}
