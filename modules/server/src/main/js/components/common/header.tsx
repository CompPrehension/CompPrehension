import { observer } from "mobx-react";
import React from "react";
import Navbar from "react-bootstrap/esm/Navbar";
import { Language } from "../../types/language";


export type HeaderProps = {
    text?: string | null,
    pagination?: React.ReactNode | React.ReactNode[] | null,
    languageHint: string,
    language: Language,
    onLanguageClicked?: (() => void) | null,
    userHint: string,
    user: string,
    userHref?: string | null,
    onUserClicked?: (() => void) | null,
}
export const Header = observer((props: HeaderProps) => {
    const { text, pagination, languageHint, language, onLanguageClicked, userHint, user, onUserClicked, userHref } = props;

    return (
        <Navbar className="px-0">
            {
                text && <h5>{text}</h5> || null
            }
            <Navbar.Collapse className="justify-content-end">
                {pagination}
                <Navbar.Text className="px-2">
                    {languageHint}: <a href="#" onClick={onLanguageClicked ?? undefined}>{language}</a>
                </Navbar.Text>
                <Navbar.Toggle />        
                <Navbar.Text className="px-2">
                    {userHint}: <a href={userHref ?? "#"} onClick={onUserClicked ?? undefined}>{user}</a>
                </Navbar.Text>
            </Navbar.Collapse>
        </Navbar>
    );
})
