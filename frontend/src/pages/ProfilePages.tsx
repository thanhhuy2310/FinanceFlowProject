import { Moon, Sun } from "lucide-react";
import { useState } from "react";

import { Button } from "../components/ui/Button";
import { PageHeader } from "../components/ui/PageHeader";
import { useAuth } from "../contexts/AuthContext";
import { formatDate } from "../utils/format";

export function ProfilePage() {
  const { user } = useAuth();

  return (
    <>
      <PageHeader title="Profile" description="Your signed-in identity and account details." />

      <section className="card card-pad detail-card profile-head">
        <span className="avatar" aria-hidden="true">
          {(user?.fullName ?? "?").trim().charAt(0).toUpperCase()}
        </span>
        <div>
          <h2>{user?.fullName ?? "—"}</h2>
          <p className="subtle">{user?.email ?? "—"}</p>
        </div>
      </section>

      <section className="card card-pad detail-card">
        <h2>Account details</h2>
        <dl className="detail-list">
          <div>
            <dt>Email</dt>
            <dd>{user?.email ?? "—"}</dd>
          </div>
          <div>
            <dt>Member since</dt>
            <dd>{user?.createdAt ? formatDate(user.createdAt) : "—"}</dd>
          </div>
        </dl>
      </section>
    </>
  );
}

const THEME_KEY = "financeflow_theme";

export function SettingsPage() {
  const [dark, setDark] = useState(() => document.documentElement.classList.contains("dark"));

  const toggleTheme = () => {
    const next = !dark;

    setDark(next);
    document.documentElement.classList.toggle("dark", next);
    localStorage.setItem(THEME_KEY, next ? "dark" : "light");
  };

  return (
    <>
      <PageHeader title="Settings" description="Control local presentation preferences." />

      <section className="card card-pad detail-card">
        <h2>Appearance</h2>
        <p className="subtle">Choose the interface theme that works best for you on this device.</p>
        <Button variant="secondary" className="action-offset" onClick={toggleTheme}>
          {dark ? <Sun size={17} /> : <Moon size={17} />}
          Switch to {dark ? "light" : "dark"} mode
        </Button>
      </section>

      <section className="card card-pad detail-card">
        <h2>About</h2>
        <dl className="detail-list">
          <div>
            <dt>Version</dt>
            <dd>1.0.0</dd>
          </div>
          <div>
            <dt>Timezone</dt>
            <dd>{Intl.DateTimeFormat().resolvedOptions().timeZone || "—"}</dd>
          </div>
        </dl>
      </section>
    </>
  );
}
