import {createRoot} from 'react-dom/client';
import React from 'react';
import "./i18n";
import "./styles/index.css";
import 'bootstrap/dist/css/bootstrap.min.css';
import {BrowserRouter as Router, Navigate, Route, Routes} from 'react-router-dom';
import {Exercise} from './pages/exercise';
import {Statistics} from './pages/statistics';
import {ExercisesList} from './pages/exercises-list';
import {SurveyPage} from './pages/survey';
import {ExerciseSettings} from './pages/exercise-settings';
import {StrategySettings} from './pages/strategy-settings';
import {QuestionPage} from './pages/question';
import {GlobalPool} from './pages/global-pool';
import {CoursePage} from './pages/course';
import {CoursesPage} from './pages/courses';
import {SessionProvider} from './hooks/session-context';
import {ErrorNotifications} from './components/common/errors';

const Home = () => (
    <>
    <ErrorNotifications />
    <SessionProvider>
        <div className="container comp-ph-container">
            <Router>
                <Routes>
                    <Route path="/pages/statistics" element={<Statistics />} />
                    <Route path="/pages/exercise" element={<Exercise />} />
                    <Route path="/pages/exercise-settings" element={<ExerciseSettings />} />
                    <Route path="/pages/strategy-settings" element={<StrategySettings />} />
                    <Route path="/pages/survey" element={<SurveyPage />} />
                    <Route path="/pages/question" element={<QuestionPage />} />
                    <Route path="/pages/exercises-list" element={<ExercisesList />} />
                    <Route path="/pages/global-pool" element={<GlobalPool />} />
                    <Route path="/pages/course" element={<CoursePage />} />
                    <Route path="/pages/courses" element={<CoursesPage />} />
                    <Route path="/" element={<Navigate to="/pages/courses" replace />} />
                </Routes>
            </Router>
        </div>
    </SessionProvider>
    </>
)

/**
 * `npm run dev:mock` answers the whole api from src/main/js/mocks via a service worker.
 * The branch is dead code in a production build, so msw never reaches the bundle.
 */
async function startMocking() {
    if (!import.meta.env.DEV || import.meta.env.VITE_MOCK_API !== 'true') {
        return;
    }
    const { worker } = await import('./mocks/browser');
    await worker.start({ onUnhandledRequest: 'bypass' });
}

const container = document.getElementById('root');
const root = createRoot(container!);
startMocking()
    // a browser that refuses to register the worker must not cost us the whole page:
    // the app still starts, it just talks to the real api
    .catch(err => console.error('[mocks] could not start, the api is NOT mocked:', err))
    .finally(() => root.render(<Home />));
