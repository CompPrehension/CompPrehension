
import React, { createContext, useContext, useEffect, useState } from 'react';
import { ReactNode } from 'react';
import { SessionStore } from '../stores/session-store';
import { observer } from 'mobx-react';
import { LoadFailure } from '../components/common/errors';

const SessionContext = createContext<SessionStore | null>(null);

export const SessionProvider = observer(({ children }: { children: ReactNode }) => {
  const [session] = useState(() => new SessionStore());

  useEffect(() => {
    session.loadSessionInfo();
  }, [session]);

  return (
    <SessionContext.Provider value={session}>
      {session.error && !session.user
        ? <div className="container pt-3">
            <LoadFailure error={session.error} onRetry={() => session.loadSessionInfo()} />
          </div>
        : children}
    </SessionContext.Provider>
  );
});

export const useSession = () => {
  const session = useContext(SessionContext);
  if (!session) {
    throw new Error("useSession must be used within a <SessionProvider>");
  }
  return session;
};

export const useCurrentUser = () => {
    const session = useSession();
    if (!session) {
        throw new Error("useCurrentUser must be used within a <SessionProvider>");
      }
    return session.user;
};
