import { LoaderCircle } from "lucide-react";
import { lazy, Suspense } from "react";
import { Navigate, Outlet, Route, Routes } from "react-router-dom";

import { useAuth } from "./contexts/AuthContext";
import { AppLayout } from "./layouts/AppLayout";

// Route-level code splitting: each page is loaded only when first visited.
const DashboardPage = lazy(() =>
  import("./pages/DashboardPage").then((module) => ({ default: module.DashboardPage })),
);
const AccountsPage = lazy(() =>
  import("./pages/AccountsPage").then((module) => ({ default: module.AccountsPage })),
);
const CategoriesPage = lazy(() =>
  import("./pages/CategoriesPage").then((module) => ({ default: module.CategoriesPage })),
);
const TransactionsPage = lazy(() =>
  import("./pages/TransactionsPage").then((module) => ({ default: module.TransactionsPage })),
);
const RulesPage = lazy(() =>
  import("./pages/RulesPage").then((module) => ({ default: module.RulesPage })),
);
const ProvidersPage = lazy(() =>
  import("./pages/ProvidersPage").then((module) => ({ default: module.ProvidersPage })),
);
const ImportBatchesPage = lazy(() =>
  import("./pages/ImportBatchesPage").then((module) => ({ default: module.ImportBatchesPage })),
);
const ProfilePage = lazy(() =>
  import("./pages/ProfilePages").then((module) => ({ default: module.ProfilePage })),
);
const SettingsPage = lazy(() =>
  import("./pages/ProfilePages").then((module) => ({ default: module.SettingsPage })),
);
const LoginPage = lazy(() =>
  import("./pages/AuthPage").then((module) => ({ default: module.LoginPage })),
);
const RegisterPage = lazy(() =>
  import("./pages/AuthPage").then((module) => ({ default: module.RegisterPage })),
);
const NotFoundPage = lazy(() =>
  import("./pages/NotFoundPage").then((module) => ({ default: module.NotFoundPage })),
);

function PageFallback() {
  return (
    <div className="page-loading" role="status" aria-label="Loading page">
      <LoaderCircle className="spin subtle" size={28} />
    </div>
  );
}

function ProtectedRoute() {
  const { token } = useAuth();

  return token ? <Outlet /> : <Navigate to="/login" replace />;
}

export default function App() {
  return (
    <Suspense fallback={<PageFallback />}>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />

        <Route element={<ProtectedRoute />}>
          <Route element={<AppLayout />}>
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/accounts" element={<AccountsPage />} />
            <Route path="/accounts/new" element={<AccountsPage />} />
            <Route path="/categories" element={<CategoriesPage />} />
            <Route path="/categories/new" element={<CategoriesPage />} />
            <Route path="/transactions" element={<TransactionsPage />} />
            <Route path="/transactions/new" element={<TransactionsPage />} />
            <Route path="/rules" element={<RulesPage />} />
            <Route path="/rules/new" element={<RulesPage />} />
            <Route path="/providers" element={<ProvidersPage />} />
            <Route path="/imports" element={<ImportBatchesPage />} />
            <Route path="/imports/new" element={<ImportBatchesPage />} />
            <Route path="/profile" element={<ProfilePage />} />
            <Route path="/settings" element={<SettingsPage />} />
          </Route>
        </Route>

        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </Suspense>
  );
}
