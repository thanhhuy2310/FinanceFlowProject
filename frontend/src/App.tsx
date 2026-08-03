import { Navigate, Outlet, Route, Routes } from "react-router-dom";
import { useAuth } from "./contexts/AuthContext";
import { AppLayout } from "./layouts/AppLayout";
import { AccountsPage } from "./pages/AccountsPage";
import { CategoriesPage } from "./pages/CategoriesPage";
import { DashboardPage } from "./pages/DashboardPage";
import { ImportBatchesPage } from "./pages/ImportBatchesPage";
import { LoginPage, RegisterPage } from "./pages/AuthPage";
import { NotFoundPage } from "./pages/NotFoundPage";
import { ProfilePage, SettingsPage } from "./pages/ProfilePages";
import { ProvidersPage } from "./pages/ProvidersPage";
import { RulesPage } from "./pages/RulesPage";
import { TransactionsPage } from "./pages/TransactionsPage";

function ProtectedRoute() {
  const { token } = useAuth();
  return token ? <Outlet/> : <Navigate to="/login" replace/>;
}

export default function App() {
  return <Routes><Route path="/login" element={<LoginPage/>}/><Route path="/register" element={<RegisterPage/>}/><Route element={<ProtectedRoute/>}><Route element={<AppLayout/>}><Route path="/dashboard" element={<DashboardPage/>}/><Route path="/accounts" element={<AccountsPage/>}/><Route path="/accounts/new" element={<AccountsPage/>}/><Route path="/categories" element={<CategoriesPage/>}/><Route path="/categories/new" element={<CategoriesPage/>}/><Route path="/transactions" element={<TransactionsPage/>}/><Route path="/transactions/new" element={<TransactionsPage/>}/><Route path="/rules" element={<RulesPage/>}/><Route path="/rules/new" element={<RulesPage/>}/><Route path="/providers" element={<ProvidersPage/>}/><Route path="/imports" element={<ImportBatchesPage/>}/><Route path="/imports/new" element={<ImportBatchesPage/>}/><Route path="/profile" element={<ProfilePage/>}/><Route path="/settings" element={<SettingsPage/>}/></Route></Route><Route path="/" element={<Navigate to="/dashboard" replace/>}/><Route path="*" element={<NotFoundPage/>}/></Routes>;
}
