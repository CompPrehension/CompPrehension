import { observer } from 'mobx-react';
import * as React from 'react';
import { Navbar } from 'react-bootstrap';
import store from '../store';
import { Optional } from './optional';
import { Pagination } from './pagination';


export const Header = observer(() => {
    const { sessionInfo, } = store;
    if (!sessionInfo) {
        return null;
    }
    const { user } = sessionInfo;
    const currentQuestionIdx = sessionInfo.questionIds.findIndex(id => store.questionData?.questionId === id);

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
                    Signed in as: <a href="#login">{user.displayName}</a>
                </Navbar.Text>
            </Navbar.Collapse>
        </Navbar>
    );
});
