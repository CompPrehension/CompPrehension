import { observer } from 'mobx-react';
import * as React from 'react';
import { Navbar } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import { Optional } from '../common/optional';
import { Pagination } from './pagination';
import { container } from "tsyringe";
import { ExerciseStore } from "../../stores/exercise-store";
import { useState } from 'react';

export const Header = observer(() => {
    const [exerciseStore] = useState(() => container.resolve(ExerciseStore));
    const { currentAttempt, sessionInfo, currentQuestion } = exerciseStore;
    if (!currentAttempt || !sessionInfo) {
        return null;
    }
    const { user } = sessionInfo;
    const currentQuestionIdx = currentAttempt.questionIds.findIndex(id => currentQuestion.question?.questionId === id);

    return (
        <Navbar className="px-0">
            <Optional condition={currentQuestionIdx !== -1}><h5>Question #{currentQuestionIdx + 1}</h5></Optional>            
            <Navbar.Collapse className="justify-content-end">   
                <Pagination />
                
                <Navbar.Text className="px-2">
                    Language: <a href="#language">{sessionInfo.language}</a>
                </Navbar.Text>     
                <Navbar.Toggle />        
                <Navbar.Text className="px-2">
                    Signed in as: <Link to={`pages/statistics?exerciseId=${exerciseStore.currentAttempt?.exerciseId}`}>{user.displayName}</Link>{/*<a href="pages/statistics">{user.displayName}</a>*/}
                </Navbar.Text>
            </Navbar.Collapse>
        </Navbar>
    );
});
