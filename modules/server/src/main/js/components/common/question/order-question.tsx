import {observer} from "mobx-react";
import React, {useEffect, useMemo} from "react";
import {useTranslation} from "react-i18next";
import {Answer} from "../../../types/answer";
import {OrderQuestionFeedback} from "../../../types/feedback";
import {OrderQuestion} from "../../../types/question";
import {isNullOrUndefined} from "../../../utils/helpers";
import {Optional} from "../optional";

type OrderQuestionComponentProps = {
    question: OrderQuestion,
    getFeedback: () => OrderQuestionFeedback | undefined, 
    answers: Answer[],
    getAnswers: () => Answer[],
    onChanged: (newAnswers: Answer[]) => void,
}

/** Choice of an answer element, from a host click or from the code of the question text. */
const ANSWER_EVALUATED_EVENT = 'compph:on-answer-evaluated';

export const OrderQuestionComponent = observer((props: OrderQuestionComponentProps) => {
    const { question, getAnswers, onChanged, getFeedback } = props; 
    const { t, i18n } = useTranslation();
    if (!question.options.requireContext) {
        return null;
    }
    const { options } = question;
    const orderNumberOptions = options.orderNumberOptions ?? { delimiter: '/', position: 'SUFFIX', }
    const originalText = useMemo(() => {
        const originalText = document.createElement('div')
        originalText.innerHTML = question.text;
        return originalText;
    }, [question.text]);

    // actions on questionId changed (onInit)
    useEffect(() => {    
        // the answer is taken from the element the event came from; the question text dispatches it for its own clicks
        const root = document.getElementById(`question_${question.questionId}`);
        const onAnswerEvaluated = (event: Event) => {
            const idStr = (event.target as Element).closest('[data-answer-id]')?.getAttribute('data-answer-id');
            if (isNullOrUndefined(idStr)) {
                return;
            }
            const id = +idStr;
            onChanged([...getAnswers(), { answer: [id, id], isCreatedByUser: true }]);
        };
        root?.addEventListener(ANSWER_EVALUATED_EVENT, onAnswerEvaluated);

        // add button click event handlers
        document.querySelectorAll(`#question_${question.questionId} [data-answer-id]`).forEach(e => {
            e.addEventListener('click', () => {
                if (!e.hasAttribute('data-comp-ph-custom-click')) {
                    e.dispatchEvent(new CustomEvent(ANSWER_EVALUATED_EVENT, { bubbles: true }));
                }
            });
        })

        // scripts inserted through innerHTML are not executed by the browser, recreated ones are
        document.querySelectorAll(`#question_${question.questionId} .comp-ph-question-text script:not([data-comp-ph-executed])`).forEach(original => {
            const script = document.createElement('script');
            Array.from(original.attributes).forEach(({ name, value }) => script.setAttribute(name, value));
            script.setAttribute('data-comp-ph-executed', '');
            script.textContent = original.textContent;
            original.replaceWith(script);
        })

        // show elements values
        document.querySelectorAll('[data-comp-ph-value]').forEach(e => {
            const value = e.getAttribute('data-comp-ph-value');
            e.innerHTML += `<span class="comp-ph-expr-bottom-hint">${value}</span>`;
        })

        // show elements positions
        document.querySelectorAll('[data-comp-ph-pos]').forEach(e => {
            const pos = e.getAttribute('data-comp-ph-pos');
            e.innerHTML += `<span class="comp-ph-expr-top-hint">${pos}</span>`;
        })

        return () => root?.removeEventListener(ANSWER_EVALUATED_EVENT, onAnswerEvaluated);
        // listeners are attached once per question
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [question.questionId]);

    useEffect(() => {
        document.querySelectorAll(`#question_${question.questionId} .comp-ph-question-text [data-localization-key]`).forEach(e => {
            const key = e.getAttribute('data-localization-key') ?? '';
            // the text from the question is kept to return to it for languages without a translation
            if (!e.hasAttribute('data-localization-default')) {
                e.setAttribute('data-localization-default', e.textContent ?? '');
            }
            e.textContent = i18n.exists(key) ? t(key) : e.getAttribute('data-localization-default');
        })
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [question.questionId, i18n.language]);

    const answersCount = getAnswers().length;
    useEffect(() => {
        // drop all changes, set original qustion text    
        document.querySelectorAll(`#question_${question.questionId} [data-answer-id]`).forEach(e => {
            const value = e.getAttribute('data-comp-ph-value');
            const pos = e.getAttribute("data-comp-ph-pos");
            e.innerHTML = originalText.querySelector(`#${e.id}`)?.innerHTML + (pos ? `<span class="comp-ph-expr-top-hint">${pos}</span>` : '') + (value ? `<span class="comp-ph-expr-bottom-hint">${value}</span>` : '')
            e.classList.remove('disabled');
            e.classList.remove('comp-ph-question-answer--last-selected-by-system');
        });

        // calc last generated by system answers
        const isLastAsnwerCorrect = getFeedback()?.isCorrect ?? true;
        const lastGeneratedAnswers = !isLastAsnwerCorrect
            ? []
            : [...getAnswers()].reverse().reduce<[number, Answer[]]>(((acc, answer) => {
                const [prevInteractionId, result] = acc;
                if (prevInteractionId === -1)
                    return acc;
                if (answer.isCreatedByUser || prevInteractionId !== 0 && prevInteractionId !== answer.createdByInteraction)
                    return [-1, result];
                result.push(answer);
                return [answer.createdByInteraction || -1, result];
            }), [0, []])[1].reverse();

        // apply history changes    
        getAnswers().forEach((answer, idx) => {
            const { answer:asnwerPair } = answer;
            const [h] = asnwerPair;
            const answrs = document.querySelectorAll(`[data-answer-id='${h}']`);
            if (!answrs.length) {
                return 0;
            }

            answrs.forEach(answr => {
                // an answer may show its step on another answer element, e.g. a hidden dropdown item on its button
                const orderAnswerId = answr.getAttribute('data-comp-ph-order-answer');
                const numbered = (orderAnswerId === null
                    ? null
                    : document.querySelector(`#question_${question.questionId} [data-answer-id='${orderAnswerId}']`)) ?? answr;
                if (lastGeneratedAnswers.includes(answer)) {
                    numbered.classList.add('comp-ph-question-answer--last-selected-by-system');
                }

                // add pos hint
                if (orderNumberOptions.position !== 'NONE') {
                    const delim = orderNumberOptions.delimiter;
                    const orderNumber = orderNumberOptions.replacers?.[idx] ?? (idx + 1);
                    const answerHtml = orderNumberOptions.position === 'PREFIX' ? `${orderNumber}${delim}${numbered.innerHTML}` :
                                    orderNumberOptions.position === 'SUFFIX' ? `${numbered.innerHTML}${delim}${orderNumber}` :
                                    orderNumberOptions.position === 'BOTTOM' ? `<span class="comp-ph-expr-bottom-hint">${delim}${orderNumber}</span>${numbered.innerHTML}` :
                                    numbered.innerHTML;
                   numbered.innerHTML = answerHtml;
                }
                // disable if needed
                if (!options.multipleSelectionEnabled) {            
                    answr.classList.add('disabled');
                }
            })            
        });
        // answersCount stands in for the answers themselves: the callbacks are new on
        // every render, the count changes only when a step is taken
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [question.questionId, answersCount])
    
    const trace = getFeedback()?.trace ?? (getAnswers().length === 0 ? question.initialTrace : null);
    const isTraceVisible = options.showTrace && !isNullOrUndefined(trace) && trace.length > 0;

    return (
        <div id={`question_${question.questionId}`} className="comp-ph-question">
            <div className="comp-ph-question-text" dangerouslySetInnerHTML={{ __html: question.text }} />
            <Optional isVisible={isTraceVisible}>
                <div>
                    <table className="comp-ph-trace">
                        <tbody>                            
                            {trace?.map((t, idx) => <tr key={idx}><td dangerouslySetInnerHTML={{ __html: t }}></td></tr>)}                            
                        </tbody>
                    </table>
                </div>
            </Optional>
        </div>
    );
})
