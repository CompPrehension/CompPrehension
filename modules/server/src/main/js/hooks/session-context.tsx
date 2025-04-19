
import React, { createContext, useContext, useEffect, useState } from 'react';
import { ReactNode } from 'react';
import { SessionStore } from '../stores/session-store';

const SessionContext = createContext<SessionStore | null>(null);

export const SessionProvider = ({ children }: { children: ReactNode }) => {
  const [session] = useState(() => new SessionStore());

  useEffect(() => {
    session.loadSessionInfo();
  }, [session]);

  return (
    <SessionContext.Provider value={session}>
      {children}
    </SessionContext.Provider>
  );
};

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
