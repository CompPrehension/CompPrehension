import { observer } from 'mobx-react';
import { useEffect, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { useSession } from '../hooks/session-context';
import { useTour } from './TourProvider';

export const TourLauncher = observer(() => {
  const { start, isReady } = useTour();
  const { selectedLanguage } = useSession();
  const { i18n } = useTranslation();
  const isLanguageReady = selectedLanguage !== undefined && i18n.language === selectedLanguage;
  const hasStarted = useRef(false);

  useEffect(() => {
    if (!isReady || !isLanguageReady || hasStarted.current) return;
    const shown = localStorage.getItem('tour_completed');
    if (shown !== 'true') {
      hasStarted.current = true;
      start(); // запускаем тур
    }
  }, [isReady, isLanguageReady, start]);

  return null; // этот компонент ничего не рендерит
});
