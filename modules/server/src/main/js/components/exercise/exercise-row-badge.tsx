import React from 'react';

export type ExerciseLinkType = 'global' | 'original' | 'inherited' | 'cloned';

const labels: Record<ExerciseLinkType, string> = {
    global: 'Global',
    original: 'Course-private',
    inherited: 'Inherited from global',
    cloned: 'Cloned from global',
};

const variants: Record<ExerciseLinkType, string> = {
    global: 'badge-warning',
    original: 'badge-success',
    inherited: 'badge-info',
    cloned: 'badge-secondary',
};

export const ExerciseRowBadge: React.FC<{ linkType: ExerciseLinkType }> = ({ linkType }) => (
    <span className={`badge ${variants[linkType]}`}>{labels[linkType]}</span>
);
