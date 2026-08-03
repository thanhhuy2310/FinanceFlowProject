import { FileUp, Landmark, LayoutDashboard, LogOut, PiggyBank, Settings, SlidersHorizontal, Tags, UserRound, WalletCards, CreditCard } from "lucide-react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";

const navigation = [
  ["/dashboard", "Dashboard", LayoutDashboard], ["/accounts", "Accounts", WalletCards], ["/transactions", "Transactions", CreditCard],
  ["/categories", "Categories", Tags], ["/rules", "Rules", SlidersHorizontal], ["/imports", "Imports", FileUp], ["/providers", "Providers", Landmark],
] as const;

export function AppLayout() {
  const navigate = useNavigate();
  const { signOut, user } = useAuth();
  const logout = () => { signOut(); navigate("/login", { replace: true }); };
  return <div className="app-shell"><a className="skip-link" href="#main-content">Skip to content</a><aside className="sidebar"><NavLink className="brand" to="/dashboard"><span className="brand-mark"><PiggyBank size={18}/></span><span>FinanceFlow</span></NavLink><nav className="nav" aria-label="Primary navigation">{navigation.map(([to, label, Icon]) => <NavLink key={to} to={to} className="nav-link"><Icon size={18}/><span>{label}</span></NavLink>)}</nav><nav className="nav nav-bottom" aria-label="Account navigation"><NavLink to="/settings" className="nav-link"><Settings size={18}/><span>Settings</span></NavLink><button className="nav-link nav-button" onClick={logout}><LogOut size={18}/><span>Sign out</span></button></nav></aside><div className="main"><header className="topbar"><span className="workspace-label">Personal finance workspace</span><NavLink className="profile-link" to="/profile"><span className="avatar" aria-hidden="true">{user?.fullName?.slice(0, 1).toUpperCase() ?? <UserRound size={16}/>}</span><span>{user?.fullName ?? "Profile"}</span></NavLink></header><main id="main-content" className="content"><Outlet/></main></div></div>;
}
