import { createContext, useContext, useMemo, useState, type ReactNode } from "react";
import type { User } from "../types/api";

const tokenKey = "financeflow_token";
const userKey = "financeflow_user";
type AuthContextValue = { token: string | null; user: User | null; signIn: (token: string, user: User) => void; signOut: () => void };
const AuthContext = createContext<AuthContextValue | null>(null);

function readUser() {
  try { return JSON.parse(localStorage.getItem(userKey) ?? "null") as User | null; } catch { return null; }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem(tokenKey));
  const [user, setUser] = useState<User | null>(readUser);
  const value = useMemo<AuthContextValue>(() => ({
    token,
    user,
    signIn: (nextToken, nextUser) => { localStorage.setItem(tokenKey, nextToken); localStorage.setItem(userKey, JSON.stringify(nextUser)); setToken(nextToken); setUser(nextUser); },
    signOut: () => { localStorage.removeItem(tokenKey); localStorage.removeItem(userKey); setToken(null); setUser(null); },
  }), [token, user]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used within AuthProvider");
  return context;
}
