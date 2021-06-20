import { observer } from 'mobx-react';
import * as React from 'react';
import { useState } from 'react';
import { Pagination as BootstrapPagination } from "react-bootstrap"
import { container } from "tsyringe";
import { ExerciseStore } from "../../stores/exercise-store";
import { Optional } from '../common/optional';

export const Pagination = observer(() => {
    const [exerciseStore] = useState(() => container.resolve(ExerciseStore));
    if (!exerciseStore.currentQuestion.question || !exerciseStore.currentAttempt) {
        return null;
    }

    const { questionId:currentQuestionId } = exerciseStore.currentQuestion.question;
    const { questionIds } = exerciseStore.currentAttempt;

    return (
        <div className="d-flex justify-content-center">
            <BootstrapPagination className="p-3" style={{ marginBottom: '0 !important' }}>
                <Optional isVisible={questionIds.length >= 5}>
                    <BootstrapPagination.First disabled={currentQuestionId === questionIds[0]} onClick={() => exerciseStore.currentQuestion.loadQuestion(questionIds[0])}/>
                    <BootstrapPagination.Prev disabled={currentQuestionId === questionIds[0]} onClick={() => exerciseStore.currentQuestion.loadQuestion(questionIds[questionIds.indexOf(currentQuestionId) - 1])}/>
                </Optional>
                {questionIds.map((id, idx) => 
                    (<BootstrapPagination.Item key={idx + 1} 
                                               active={currentQuestionId === id}
                                               onClick={() => exerciseStore.currentQuestion.loadQuestion(id)}>
                        {idx + 1}
                     </BootstrapPagination.Item>))}
                <Optional isVisible={questionIds.length >= 5}>
                    <BootstrapPagination.Next disabled={currentQuestionId === questionIds[questionIds.length - 1]} onClick={() => exerciseStore.currentQuestion.loadQuestion(questionIds[questionIds.indexOf(currentQuestionId) + 1])}/>
                    <BootstrapPagination.Last disabled={currentQuestionId === questionIds[questionIds.length - 1]} onClick={() => exerciseStore.currentQuestion.loadQuestion(questionIds[questionIds.length - 1])}/>
                </Optional>
            </BootstrapPagination>
        </div>
    );
})
