import React, { useEffect, useState } from "react";
import { observer } from "mobx-react";
import { exerciseStore } from '../../../../stores/exercise-store';

export const OrderQuestion : React.FC = observer(() => {
    const { currentQuestion, answersHistory, onAnswersChanged } = exerciseStore;
    if (!currentQuestion || currentQuestion.type != "ORDER" || !currentQuestion.options.requireContext) {
        return null;
    }

    const { options } = currentQuestion; 
    const orderNumberOptions = options.orderNumberOptions ?? { delimiter: '/', position: 'SUFFIX', }
    
    const originalText = document.createElement('div');
    originalText.innerHTML = currentQuestion.text;
    
    // actions on questionId changed (onInit)
    useEffect(() => {    
        // add button click event handlers
        document.querySelectorAll('[id^="answer_"]').forEach(e => {
            const idStr = e.id?.split("answer_")[1] ?? ""; 
            const id = +idStr;
            e.addEventListener('click', () => onAnswersChanged([id, id]));
        })

        // show elements positions
        document.querySelectorAll('[data-comp-ph-pos]').forEach(e => {
            const pos = e.getAttribute('data-comp-ph-pos');
            e.innerHTML += `<span class="comp-ph-expr-top-hint">${pos}</span>`;
        })

    }, [currentQuestion.questionId]);

    useEffect(() => {
        // drop all changes, set original qustion text    
        document.querySelectorAll('[id^="answer_"]').forEach(e => {
            const pos = e.getAttribute("data-comp-ph-pos");
            e.innerHTML = originalText.querySelector(`#${e.id}`)?.innerHTML + `<span class="comp-ph-expr-top-hint">${pos}</span>`
            e.classList.remove('disabled');
        });

        // apply history changes    
        answersHistory.forEach(([h], idx) => {
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
    }, [currentQuestion.questionId, exerciseStore.answersHistory.length])
    

    return (
        <div>
            <div dangerouslySetInnerHTML={{ __html: currentQuestion.text }} />
        </div>
    );
});
