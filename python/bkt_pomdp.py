"""
Функциональная реализация Teaching-режима BKT-POMDP
(см. DOI 10.24963/ijcai.2021/583)
"""
import pandas as pd
from itertools import combinations
from typing import Dict, List, Tuple, Iterable, Optional
from pyBKT.models import Roster


def _get_param(df: pd.DataFrame, skill: str,
               aliases: Tuple[str, ...]) -> float:
    """
    Берём значение параметра по приоритету:
        1) строка (skill, alias, 'default')
        2) среднее по всем классам alias
    """
    for a in aliases:
        idx_default = (skill, a, 'default')
        if idx_default in df.index:
            return float(df.loc[idx_default, 'value'])

        # иначе среднее по всем классам alias
        try:
            return float(df.loc[(skill, a)]['value'].mean())
        except KeyError:
            continue
    raise ValueError(f'Param {aliases} not fount for skill {skill}')


def extract_skills_params(roster: Roster,
                          skills: List[str]) -> Dict[str, Tuple[float, float, float]]:
    """
    Возвращает словарь {skill: (guess, slip, learn)}
    """
    df: pd.DataFrame = roster.model.params().sort_index()
    df = df.copy()
    df.index.set_names(['skill', 'param', 'class'], inplace=True)

    params = {}
    for skill in skills:
        g = _get_param(df, skill, ('guesses', 'guess'))
        s = _get_param(df, skill, ('slips', 'slip'))
        l = _get_param(df, skill, ('learns', 'learn'))
        params[skill] = (g, s, l)
    return params


def enumerate_questions(skills: List[str],
                        max_count: int) -> List[Tuple[str, ...]]:
    """Все сочетания от 1 до max_count."""
    result = []
    for k in range(1, max_count + 1):
        result.extend(combinations(skills, k))
    return result


def exp_posterior(b: float, g: float, s: float, l: float) -> float:
    """
    E[b'] по формулам (3)(5)(9) статьи «BKT-POMDP».
    """
    p_corr = b * (1 - s) + (1 - b) * g
    if 0.0 < p_corr < 1.0:
        p_knw_c = (b * (1 - s)) / p_corr
        p_knw_i = (b * s) / (b * s + (1 - b) * (1 - g))
    else:
        p_knw_c = p_knw_i = b

    p_post_c = p_knw_c + (1 - p_knw_c) * l
    p_post_i = p_knw_i + (1 - p_knw_i) * l
    return p_corr * p_post_c + (1 - p_corr) * p_post_i


def expected_reward(question: Tuple[str, ...],
                    beliefs: Dict[str, float],
                    params: Dict[str, Tuple[float, float, float]]) -> float:
    """Суммарный положительный прирост знания (формула 8)."""
    reward = 0.0
    for skill in question:
        b = beliefs[skill]
        g, s, l = params[skill]
        reward += max(0.0, exp_posterior(b, g, s, l) - b)
    return reward


def choose_best_question(
    roster: Roster,
    all_skills: List[str],
    student: str,
    max_question_skills_count: int = 5,
    candidate_questions: Optional[Iterable[Tuple[str, ...]]] = None,
) -> List[str]:
    """
    Возвращает оптимальный набор навыков для следующего вопроса.
    """
    # 1. параметры навыков
    skills_params = extract_skills_params(roster, all_skills)

    # 2. актуальные вероятности мастерства ученика
    beliefs = {
        skill: float(roster.get_mastery_prob(skill, student))
        for skill in all_skills
    }

    # 3. пул вопросов
    if candidate_questions is None:
        candidate_questions = enumerate_questions(all_skills, max_question_skills_count)

    # 4. выбор максимального ожидаемого прироста
    best_q, best_r = (), -1.0
    for q in candidate_questions:
        r = expected_reward(q, beliefs, skills_params)
        if r > best_r:
            best_q, best_r = q, r
    return list(best_q)
