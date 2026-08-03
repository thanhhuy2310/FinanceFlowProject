import { Moon, Sun } from "lucide-react";
import { useState } from "react";
import { Button } from "../components/ui/Button";
import { PageHeader } from "../components/ui/PageHeader";
import { useAuth } from "../contexts/AuthContext";

export function ProfilePage() { const { user } = useAuth(); return <><PageHeader title="Profile" description="Your current signed-in identity."/><section className="card card-pad detail-card"><h2>Account details</h2><dl className="detail-list"><div><dt>Name</dt><dd>{user?.fullName ?? "—"}</dd></div><div><dt>Email</dt><dd>{user?.email ?? "—"}</dd></div><div><dt>Role</dt><dd>{user?.role ?? "—"}</dd></div></dl></section></>; }
export function SettingsPage() {
  const [dark, setDark] = useState(() => document.documentElement.classList.contains("dark"));
  const toggleTheme = () => {
    const next = !dark;
    setDark(next);
    document.documentElement.classList.toggle("dark", next);
    localStorage.setItem("financeflow_theme", next ? "dark" : "light");
  };
  return <><PageHeader title="Settings" description="Control local presentation preferences."/><section className="card card-pad detail-card"><h2>Appearance</h2><p className="subtle">Choose the interface theme that works best for you on this device.</p><Button variant="secondary" className="action-offset" onClick={toggleTheme}>{dark ? <Sun size={17}/> : <Moon size={17}/>}Toggle light mode</Button></section></>;
}
