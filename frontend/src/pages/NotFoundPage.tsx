import {Link} from "react-router-dom";

export function NotFoundPage() {
    return <main className="auth-shell">
        <section className="card auth-card"><p className="eyebrow">404</p><h1>Page not found</h1><p
            className="subtle">The page you requested does not exist or has moved.</p><Link
            className="button button-primary action-offset" to="/dashboard">Return to dashboard</Link></section>
    </main>;
}
