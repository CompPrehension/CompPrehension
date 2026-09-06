import { useEffect } from 'react';
import { useTour } from './TourProvider';

export const TourLauncher = () => {
  const { start, isReady } = useTour();

  useEffect(() => {
    if (!isReady) return;
    const shown = localStorage.getItem('tour_completed');
    if (shown !== 'true') {
      start(); // запускаем тур
    }
  }, [isReady]);

  return null; // этот компонент ничего не рендерит
};