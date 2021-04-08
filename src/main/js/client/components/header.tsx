import { observer } from 'mobx-react';
import * as React from 'react';
import { Navbar } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import { exerciseStore } from "../stores/exercise-store";
import { Optional } from './optional';
import { Pagination } from './pagination';


export const Header = observer(() => {
    const { currentAttempt, sessionInfo, currentQuestion } = exerciseStore;
    if (!currentAttempt || !sessionInfo) {
        return null;
    }
    const { user } = sessionInfo;
    const currentQuestionIdx = currentAttempt.questionIds.findIndex(id => currentQuestion?.questionId === id);

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
