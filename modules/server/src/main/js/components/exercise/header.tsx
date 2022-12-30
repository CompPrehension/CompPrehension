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
import { Header } from '../common/header';

export const ExerciseHeader = observer(() => {
    const [exerciseStore] = useState(() => container.resolve(ExerciseStore));
    const { t, i18n } = useTranslation();
    const { currentAttempt, exercise, user, currentQuestion } = exerciseStore;
    if (!currentAttempt || !exercise || !user) {
        return null;
    }
    const currentQuestionIdx = currentAttempt.questionIds.findIndex(id => currentQuestion.question?.questionId === id);
    
    const onLangClicked = () => {
        const currentLang = user.language;
        const newLang = currentLang === "RU" ? "EN" : "RU";
        exerciseStore.changeLanguage(newLang);
    }

    return (
        <Header
            text={currentQuestionIdx !== -1 ? t('question_header', { questionNumber: currentQuestionIdx + 1 }) : ''}
            pagination={<Pagination />}
            languageHint={t('language_header')}
            language={user.language}
            userHint={t('signedin_as_header')}
            user={user.displayName}
        />
    );
});
