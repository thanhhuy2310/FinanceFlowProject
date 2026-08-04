import {
  CreditCard,
  FileUp,
  Landmark,
  LayoutDashboard,
  LogOut,
  PiggyBank,
  Settings,
  SlidersHorizontal,
  Tags,
  UserRound,
  WalletCards,
} from "lucide-react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";

import { useAuth } from "../contexts/AuthContext";

interface NavigationItem {
  to: string;
  label: string;
  icon: typeof LayoutDashboard;
}

const navigation: NavigationItem[] = [
  { to: "/dashboard", label: "Dashboard", icon: LayoutDashboard },
  { to: "/accounts", label: "Accounts", icon: WalletCards },
  { to: "/transactions", label: "Transactions", icon: CreditCard },
  { to: "/categories", label: "Categories", icon: Tags },
  { to: "/rules", label: "Rules", icon: SlidersHorizontal },
  { to: "/imports", label: "Imports", icon: FileUp },
  { to: "/providers", label: "Providers", icon: Landmark },
];

export function AppLayout() {
  const navigate = useNavigate();
  const { signOut, user } = useAuth();

  const handleSignOut = () => {
    signOut();
    navigate("/login", { replace: true });
  };

  return (
    <div className="app-shell">
      <a className="skip-link" href="#main-content">
        Skip to content
      </a>

      <aside className="sidebar">
        <NavLink className="brand" to="/dashboard">
          <span className="brand-mark">
            <PiggyBank size={18} />
          </span>
          <span>FinanceFlow</span>
        </NavLink>

        <nav className="nav" aria-label="Primary navigation">
          {navigation.map(({ to, label, icon: Icon }) => (
            <NavLink key={to} to={to} className="nav-link">
              <Icon size={18} />
              <span>{label}</span>
            </NavLink>
          ))}
        </nav>

        <nav className="nav nav-bottom" aria-label="Account navigation">
          <NavLink to="/settings" className="nav-link">
            <Settings size={18} />
            <span>Settings</span>
          </NavLink>

          <button type="button" className="nav-link nav-button" onClick={handleSignOut}>
            <LogOut size={18} />
            <span>Sign out</span>
          </button>
        </nav>
      </aside>

      <div className="main">
        <header className="topbar">
          <span className="workspace-label">Personal finance workspace</span>

          <NavLink className="profile-link" to="/profile">
            <span className="avatar" aria-hidden="true">
              {user?.fullName?.slice(0, 1).toUpperCase() ?? <UserRound size={16} />}
            </span>
            <span>{user?.fullName ?? "Profile"}</span>
          </NavLink>
        </header>

        <main id="main-content" className="content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
