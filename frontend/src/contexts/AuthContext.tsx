import { createContext, useContext, useMemo, useState, type ReactNode } from "react";

import type { User } from "../types/api";

const TOKEN_KEY = "financeflow_token";
const USER_KEY = "financeflow_user";

interface AuthContextValue {
  token: string | null;
  user: User | null;
  signIn: (token: string, user: User) => void;
  signOut: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function readStoredUser(): User | null {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) ?? "null") as User | null;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  // Lazy initializers keep the stored session available on first render.
  const [token, setToken] = useState<string | null>(() => localStorage.getItem(TOKEN_KEY));
  const [user, setUser] = useState<User | null>(readStoredUser);

  const value = useMemo<AuthContextValue>(
    () => ({
      token,
      user,
      signIn: (nextToken, nextUser) => {
        localStorage.setItem(TOKEN_KEY, nextToken);
        localStorage.setItem(USER_KEY, JSON.stringify(nextUser));
        setToken(nextToken);
        setUser(nextUser);
      },
      signOut: () => {
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(USER_KEY);
        setToken(null);
        setUser(null);
      },
    }),
    [token, user],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }

  return context;
}
