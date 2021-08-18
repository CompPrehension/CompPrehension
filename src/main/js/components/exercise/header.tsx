import { observer } from 'mobx-react';
import * as React from 'react';
import { Navbar } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import { Optional } from '../common/optional';
import { Pagination } from './pagination';
import { container } from "tsyringe";
import { ExerciseStore } from "../../stores/exercise-store";
import { useState } from 'react';
import { useTranslation } from "react-i18next";

export const Header = observer(() => {
    const [exerciseStore] = useState(() => container.resolve(ExerciseStore));
    const { t, i18n } = useTranslation();
    const { currentAttempt, sessionInfo, currentQuestion } = exerciseStore;
    if (!currentAttempt || !sessionInfo) {
        return null;
    }
    const { user } = sessionInfo;
    const currentQuestionIdx = currentAttempt.questionIds.findIndex(id => currentQuestion.question?.questionId === id);
    
    /*
    const onLangClicked = () => {
        const currentLang = sessionInfo.language;
        const newLang = currentLang === "RU" ? "EN" : "RU";
        exerciseStore.changeLanguage(newLang);
    }
    */

    return (
        <Navbar className="px-0">
            <Optional isVisible={currentQuestionIdx !== -1}>
                <h5>{t('question_header')} #{currentQuestionIdx + 1}</h5>
            </Optional>            
            <Navbar.Collapse className="justify-content-end">   
                <Pagination />
                
                <Navbar.Text className="px-2">
                    {t('language_header')}: <a href="#" /*onClick={onLangClicked}*/>{sessionInfo.language}</a>
                </Navbar.Text>     
                <Navbar.Toggle />        
                <Navbar.Text className="px-2">
                {t('signedin_as_header')}: <Link to={`statistics?exerciseId=${exerciseStore.currentAttempt?.exerciseId}`}>{user.displayName}</Link>{/*<a href="pages/statistics">{user.displayName}</a>*/}
                </Navbar.Text>
            </Navbar.Collapse>
        </Navbar>
    );
});
