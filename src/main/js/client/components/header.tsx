import { observer } from 'mobx-react';
import * as React from 'react';
import { Navbar } from 'react-bootstrap';
import store from '../store';
import { UserInfo } from '../types/user-info';


export const Header = observer(() => {
    const { sessionInfo } = store;
    if (!sessionInfo) {
        return null;
    }
    const { user } = sessionInfo;

    return (
        <Navbar>
            <Navbar.Collapse className="justify-content-end">   
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
