import React from "react";
import { useEffect } from "react";
import { OrderQuestion } from "../../../types/question";

type OrderQuestionComponentProps = {
    question: OrderQuestion,
    answers: [number, number][],
    onChanged: (newAnswers: [number, number][]) => void,
}
export const OrderQuestionComponent = ({ question, answers, onChanged }: OrderQuestionComponentProps) => {
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
        document.querySelectorAll('[id^="answer_"]').forEach(e => {
            const idStr = e.id?.split("answer_")[1] ?? ""; 
            const id = +idStr;
            e.addEventListener('click', () => onChanged([...answers, [id, id]]));
        })

        // show elements positions
        document.querySelectorAll('[data-comp-ph-pos]').forEach(e => {
            const pos = e.getAttribute('data-comp-ph-pos');
            e.innerHTML += `<span class="comp-ph-expr-top-hint">${pos}</span>`;
        })

    }, [question.questionId]);

    useEffect(() => {
        // drop all changes, set original qustion text    
        document.querySelectorAll('[id^="answer_"]').forEach(e => {
            const pos = e.getAttribute("data-comp-ph-pos");
            e.innerHTML = originalText.querySelector(`#${e.id}`)?.innerHTML + `<span class="comp-ph-expr-top-hint">${pos}</span>`
            e.classList.remove('disabled');
        });

        // apply history changes    
        answers.forEach(([h], idx) => {
            const answr = document.querySelector(`#answer_${h}`);
            if (!answr) {
                return;
            }

            // add pos hint        
            if (orderNumberOptions.position !== 'NONE') {
                const orderNumber = orderNumberOptions.replacers?.[idx] ?? (idx + 1);
                const answerHtml = orderNumberOptions.position === 'PREFIX' ? [orderNumber, answr.innerHTML] :
                                orderNumberOptions.position === 'SUFFIX' ? [answr.innerHTML, orderNumber] : [answr.innerHTML];            
                answr.innerHTML = answerHtml.join(orderNumberOptions.delimiter);
            }  
            // disable if needed
            if (options.multipleSelectionEnabled) {            
                answr.classList.add('disabled');            
            }
        });
    }, [question.questionId, answers.length])
    

    return (
        <div>
            <div dangerouslySetInnerHTML={{ __html: question.text }} />
        </div>
    );
}
