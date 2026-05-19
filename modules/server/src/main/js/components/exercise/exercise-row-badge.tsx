import React from 'react';
import { useTranslation } from 'react-i18next';

export type ExerciseLinkType = 'global' | 'original' | 'inherited' | 'cloned';

const variants: Record<ExerciseLinkType, string> = {
    global: 'badge-warning',
    original: 'badge-success',
    inherited: 'badge-info',
    cloned: 'badge-secondary',
};

export const ExerciseRowBadge: React.FC<{ linkType: ExerciseLinkType }> = ({ linkType }) => {
    const { t } = useTranslation();
    return <span className={`badge ${variants[linkType]}`}>{t(`exerciseBadge_${linkType}`)}</span>;
};
