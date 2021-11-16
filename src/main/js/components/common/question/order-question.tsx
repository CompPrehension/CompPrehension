import { observer } from "mobx-react";
import React from "react";
import { useEffect } from "react";
import { Answer } from "../../../types/answer";
import { OrderQuestionFeedback } from "../../../types/feedback";
import { OrderQuestion } from "../../../types/question";
import { notNulAndUndefinded } from "../../../utils/helpers";
import { Optional } from "../optional";

type OrderQuestionComponentProps = {
    question: OrderQuestion,
    getFeedback: () => OrderQuestionFeedback | undefined, 
    answers: Answer[],
    getAnswers: () => Answer[],
    onChanged: (newAnswers: Answer[]) => void,
}
export const OrderQuestionComponent = observer((props: OrderQuestionComponentProps) => {
    const { question, getAnswers, onChanged, getFeedback } = props; 
    if (!question.options.requireContext) {
        return null;
    }
    const { options } = question;
    const orderNumberOptions = options.orderNumberOptions ?? { delimiter: '/', position: 'SUFFIX', }
    const originalText = document.createElement('div');
    originalText.innerHTML = question.text;

    // actions on questionId changed (onInit)
    useEffect(() => {    
        // add button click event handlers
        document.querySelectorAll(`[id^="question_${question.questionId}_answer_"]`).forEach(e => {
            const idStr = e.id?.split(`question_${question.questionId}_answer_`)[1] ?? ""; 
            const id = +idStr;
            e.addEventListener('click', () => { 
                const curentAnswers = getAnswers();
                let newAnswers = [...curentAnswers, { answer: [id, id] as [number, number], isСreatedByUser: true }];
                if (!options.multipleSelectionEnabled && ([1, 2].includes(getFeedback()?.stepsLeft ?? -1) || getFeedback() === undefined && question.answers.length === 2)) {
                    const missingAnswer = question.answers
                        .find(aid => !curentAnswers.some(cid => aid.id === cid.answer[0]) && aid.id !== id);
                    if (missingAnswer) {
                        newAnswers.push({ answer: [missingAnswer.id, missingAnswer.id] as [number, number], isСreatedByUser: true })
                    }
                }
                onChanged(newAnswers);
            });
        })

        // show elements positions
        document.querySelectorAll('[data-comp-ph-pos]').forEach(e => {
            const pos = e.getAttribute('data-comp-ph-pos');
            e.innerHTML += `<span class="comp-ph-expr-top-hint">${pos}</span>`;
        })

    }, [question.questionId]);

    useEffect(() => {
        // drop all changes, set original qustion text    
        document.querySelectorAll(`[id^="question_${question.questionId}_answer_"]`).forEach(e => {
            const pos = e.getAttribute("data-comp-ph-pos");
            e.innerHTML = originalText.querySelector(`#${e.id}`)?.innerHTML + (pos ? `<span class="comp-ph-expr-top-hint">${pos}</span>` : '')
            e.classList.remove('disabled');
            //e.classList.remove('comp-ph-question-answer--selected-by-system');
        });

        // apply history changes    
        getAnswers().forEach(({ answer, isСreatedByUser }, idx, answers) => {
            const [h] = answer;
            const answr = document.querySelector(`#question_${question.questionId}_answer_${h}`);
            if (!answr) {
                return 0;
            }

            /*
            answr.classList.add('comp-ph-question-answer--selected');
            if (idx === answers.length - 1) {
                answr.classList.add('comp-ph-question-answer--last-selected');
            }
            */

            // add pos hint        
            if (orderNumberOptions.position !== 'NONE') {
                const orderNumber = orderNumberOptions.replacers?.[idx] ?? (idx + 1);
                const answerHtml = orderNumberOptions.position === 'PREFIX' ? [orderNumber, answr.innerHTML] :
                                orderNumberOptions.position === 'SUFFIX' ? [answr.innerHTML, orderNumber] : [answr.innerHTML];            
                answr.innerHTML = answerHtml.join(orderNumberOptions.delimiter);
            }  
            // disable if needed
            if (!options.multipleSelectionEnabled) {            
                answr.classList.add('disabled');
            }
        });
    }, [question.questionId, getAnswers().length])
    
    const trace = getFeedback()?.trace ?? (getAnswers().length === 0 ? question.initialTrace : null);
    const isTraceVisible = options.showTrace && notNulAndUndefinded(trace) && trace.length > 0;

    return (
        <div>
            <div className="comp-ph-question-text" dangerouslySetInnerHTML={{ __html: question.text }} />
            <Optional isVisible={isTraceVisible}>
                <p>
                    <table className="comp-ph-trace">
                        <tbody>                            
                            {trace?.map(t => <tr><td dangerouslySetInnerHTML={{ __html: t }}></td></tr>)}                            
                        </tbody>
                    </table>
                </p>
            </Optional>
        </div>
    );
})
