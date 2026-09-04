import { observer } from 'mobx-react';
import * as React from 'react';
import { Pagination } from './pagination';
import { getExerciseStore } from "../../stores/exercise-store";
import { useTranslation } from "react-i18next";
import { Header } from '../common/header';
import { useCurrentUser } from '../../hooks/session-context';

export const ExerciseHeader = observer(() => {
    const exerciseStore = getExerciseStore();
    const { t } = useTranslation();
    const user = useCurrentUser();
    const { currentAttempt, exercise, currentQuestion } = exerciseStore;

    /*
    const onLangClicked = useCallback(() => {
        const currentLang = user?.language;
        const newLang = currentLang === "RU" ? "EN" : "RU";
        session.changeLanguage(newLang);
    }, [session, user]);
    */

    if (!currentAttempt || !exercise || !user) {
        return null;
    }
    const currentQuestionIdx = currentAttempt.questionIds.findIndex(id => currentQuestion.question?.questionId === id);

    return (
        <Header
            text={currentQuestionIdx !== -1 ? t('question_header', { questionNumber: currentQuestionIdx + 1 }) : ''}
            pagination={<Pagination />}
            languageHint={t('language_header')}
            language={user.language}
            userHint={t('signedin_as_header')}
            user={user.displayName}
            onLanguageClicked={null/*onLangClicked*/}
            logoutLabel={t('logout_header')}
        />
    );
});
