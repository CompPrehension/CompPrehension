import { createRoot } from 'react-dom/client';
import React from 'react';
import "./bootstrapper";
import "./styles/index.css";
import 'bootstrap/dist/css/bootstrap.min.css';
import { Navigate, Route, BrowserRouter as Router, Routes, redirect } from 'react-router-dom';
import { Exercise } from './pages/exercise';
import { Statistics } from './pages/statistics';
import { ExercisesList } from './pages/exercises-list';
import { SurveyPage } from './pages/survey';
import { ExerciseSettings } from './pages/exercise-settings';
import { StrategySettings } from './pages/strategy-settings';
import { QuestionPage } from './pages/question';

const Home = () => (
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
                <Route path="/" element={<Navigate to="/pages/exercise-settings" replace />} />
            </Routes>
        </Router>
    </div>
)

const container = document.getElementById('root');
const root = createRoot(container!);
root.render(<Home />);
