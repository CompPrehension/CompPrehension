import { t } from 'i18next';
import React, {
  createContext,
  ReactNode,
  useContext,
  useEffect,
  useRef,
  useState,
} from 'react';
import Shepherd, { StepOptions } from 'shepherd.js';
import 'shepherd.js/dist/css/shepherd.css';

type TourContextType = {
  start: () => void;
  tour?: InstanceType<typeof Shepherd.Tour>;
  isReady: boolean;
};

const TourContext = createContext<TourContextType | null>(null);


type TourProviderProps = {
  steps: StepOptions[];
  children: ReactNode;
};

export const TourProvider = ({ steps, children }: TourProviderProps) => {
  const tourRef = useRef<InstanceType<typeof Shepherd.Tour>>();
  const [isReady, setIsReady] = useState(false);
  const isTourCompletedRef = useRef(false);

  const bindActions = (
    tour: InstanceType<typeof Shepherd.Tour>,
    steps: StepOptions[]
  ) => {
    return steps.map((step) => {
      const completeTour = () => {
        tour.complete();
        isTourCompletedRef.current = true;
        if (localStorage.getItem('tour_completed') !== 'never') {
          localStorage.setItem('tour_completed', 'true');
        }
      };

      const buttons = step.buttons?.map((button) => {
        let text =
          typeof button.text === 'function' ? button.text() : button.text || '';

        if (text === 'next') {
          return {
            ...button,
            text: t('tour_next'),
            action: () => tour.next(),
            classes: 'shepherd-button-secondary',
          };
        }
        if (text === 'skip') {
          return {
            ...button,
            text: t('tour_skip'),
            action: () => completeTour(),
            classes: 'shepherd-button-primary',
          };
        }
        if (text === 'complete') {
          return {
            ...button,
            text: t('tour_complete'),
            action: () => completeTour(),
            classes: 'shepherd-button-primary',
          };
        }
        return button;
      });

      return { ...step, buttons };
    });
  };

  useEffect(() => {
    const tour = new Shepherd.Tour({
      defaultStepOptions: {
        scrollTo: true,
        cancelIcon: { enabled: false },
      },
      useModalOverlay: true,
    });

    tourRef.current = tour;

    tour.start = async () => {
      tour.cancel();
      (tour.steps as Array<{ destroy: () => void }>).forEach((step) =>
        step.destroy()
      );

      const stepsWithActions = bindActions(tour, steps);
      let pendingSteps = [...stepsWithActions];

      const showNextStep = async (): Promise<void> => {
        if (isTourCompletedRef.current) return; // Если тур завершён, прерываем
        if (pendingSteps.length === 0) return;

        for (let i = 0; i < pendingSteps.length; i++) {
          const step = pendingSteps[i];
          const selector =
            typeof step.attachTo === 'object' ? step.attachTo?.element : null;

          if (typeof selector === 'string') {
            const element = document.querySelector(selector);
            if (element) {
              tour.addStep(step);
              tour.show(step.id);

              pendingSteps.splice(i, 1); // Удаляем текущий шаг

              await new Promise<void>((resolve) => {
                const cleanup = () => {
                  tour.off('next', cleanup);
                  tour.off('cancel', cleanup);
                  tour.off('complete', cleanup);
                  resolve();
                };
                tour.on('next', cleanup);
                tour.on('cancel', cleanup);
                tour.on('complete', cleanup);
              });

              return showNextStep(); // Переход к следующему шагу
            }
          }
        }

        // Если ни один из оставшихся шагов пока не доступен — подождём и попробуем снова
        if (!isTourCompletedRef.current) {
          setTimeout(() => showNextStep(), 500);
        }
      };

      showNextStep();
    };

    setIsReady(true);
  }, [steps]);

  return (
    <TourContext.Provider
      value={{
        start: () => tourRef.current?.start?.(),
        tour: tourRef.current,
        isReady,
      }}
    >
      {children}
    </TourContext.Provider>
  );
};

export const useTour = () => {
  const ctx = useContext(TourContext);
  if (!ctx) throw new Error('useTour must be used within TourProvider');
  return ctx;
};
