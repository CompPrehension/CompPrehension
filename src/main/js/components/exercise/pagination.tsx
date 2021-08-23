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
    const questionIds = exerciseStore.currentAttempt.questionIds.map((id, idx) => ({ id, number: idx + 1 }));
    if (questionIds.length === 0) {
        return null;
    }

    const maxNumbersInRow = 10;
    const beginEndSliceSize = 7;
    const middleSliceSize = 5;

    const currentQuestionNumber = questionIds.find(v => v.id === currentQuestionId)?.number ?? -1;
    const currentQuestionPosition = currentQuestionNumber - middleSliceSize <= 0 ? 'BEGIN'
        : currentQuestionNumber + middleSliceSize > questionIds.length ? 'END'
        : 'MIDDLE';        
    const offset = Math.floor(middleSliceSize / 2)

    return (
        <div className="d-flex justify-content-center">
            <BootstrapPagination className="p-3" style={{ marginBottom: '0 !important' }}>
                {/* Show simple navigation for small question numbers*/}
                <Optional isVisible={questionIds.length <= maxNumbersInRow}>
                    {questionIds.map((id) => 
                        (<BootstrapPagination.Item key={id.number} 
                                                active={currentQuestionId === id.id}
                                                onClick={() => exerciseStore.currentQuestion.loadQuestion(id.id)}>
                            {id.number}
                        </BootstrapPagination.Item>))}
                </Optional>


                {/* Show complex navigation otherwise*/}            
                <Optional isVisible={questionIds.length > maxNumbersInRow}>
                    <BootstrapPagination.First disabled={currentQuestionId === questionIds[0].id} onClick={() => exerciseStore.currentQuestion.loadQuestion(questionIds[0].id)}/>
                    <BootstrapPagination.Prev disabled={currentQuestionId === questionIds[0].id} onClick={() => exerciseStore.currentQuestion.loadQuestion(questionIds[questionIds.findIndex(x => currentQuestionId === x.id) - 1].id)}/>
                    
                    {/* If the current question is in the start area */}
                    <Optional isVisible={currentQuestionPosition === 'BEGIN'}>
                        {questionIds.filter(id => id.number <= beginEndSliceSize).map((id) => 
                            (<BootstrapPagination.Item key={id.number}
                                                       active={currentQuestionId === id.id}
                                                       onClick={() => exerciseStore.currentQuestion.loadQuestion(id.id)}>
                                {id.number}
                            </BootstrapPagination.Item>))}
                        <BootstrapPagination.Ellipsis disabled />
                        <BootstrapPagination.Item key={questionIds[questionIds.length - 1].number}
                                                  active={false}
                                                  onClick={() => exerciseStore.currentQuestion.loadQuestion(questionIds[questionIds.length - 1].id)}>
                            {questionIds[questionIds.length - 1].number}
                        </BootstrapPagination.Item>
                    </Optional>
                    
                    {/* If the current question is in the middle area */}
                    <Optional isVisible={currentQuestionPosition === 'MIDDLE'}>
                        <BootstrapPagination.Item key={questionIds[0].number}
                                                  active={false}
                                                  onClick={() => exerciseStore.currentQuestion.loadQuestion(questionIds[0].id)}>
                            {questionIds[0].number}
                        </BootstrapPagination.Item>
                        <BootstrapPagination.Ellipsis disabled />
                        {questionIds.filter(id => id.number >= currentQuestionNumber - offset && id.number <= currentQuestionNumber + offset).map((id) => 
                            (<BootstrapPagination.Item key={id.number}
                                                       active={currentQuestionId === id.id}
                                                       onClick={() => exerciseStore.currentQuestion.loadQuestion(id.id)}>
                                {id.number}
                            </BootstrapPagination.Item>))}
                        <BootstrapPagination.Ellipsis disabled />
                        <BootstrapPagination.Item key={questionIds[questionIds.length - 1].number}
                                                  active={false}
                                                  onClick={() => exerciseStore.currentQuestion.loadQuestion(questionIds[questionIds.length - 1].id)}>
                            {questionIds[questionIds.length - 1].number}
                        </BootstrapPagination.Item>
                    </Optional>
                    
                    {/* If the current question is in the end area  */}
                    <Optional isVisible={currentQuestionPosition === 'END'}>
                        <BootstrapPagination.Item key={questionIds[0].number}
                                                  active={false}
                                                  onClick={() => exerciseStore.currentQuestion.loadQuestion(questionIds[0].id)}>
                            {questionIds[0].number}
                        </BootstrapPagination.Item>
                        <BootstrapPagination.Ellipsis disabled />
                        {questionIds.filter(id => id.number > questionIds.length - beginEndSliceSize).map((id) => 
                            (<BootstrapPagination.Item key={id.number}
                                                       active={currentQuestionId === id.id}
                                                       onClick={() => exerciseStore.currentQuestion.loadQuestion(id.id)}>
                                {id.number}
                            </BootstrapPagination.Item>))}
                    </Optional>
                    
                    <BootstrapPagination.Next disabled={currentQuestionId === questionIds[questionIds.length - 1].id} onClick={() => exerciseStore.currentQuestion.loadQuestion(questionIds[questionIds.findIndex(x => currentQuestionId === x.id) + 1].id)}/>
                    <BootstrapPagination.Last disabled={currentQuestionId === questionIds[questionIds.length - 1].id} onClick={() => exerciseStore.currentQuestion.loadQuestion(questionIds[questionIds.length - 1].id)}/>
                </Optional>
            </BootstrapPagination>
        </div>
    );
})
