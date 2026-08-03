import { zodResolver } from "@hookform/resolvers/zod";
import { PiggyBank } from "lucide-react";
import { useForm } from "react-hook-form";
import { NavLink, Navigate, useNavigate } from "react-router-dom";
import { z } from "zod";
import { api, errorMessage, unwrap } from "../api/client";
import { Button } from "../components/ui/Button";
import { InputField } from "../components/ui/FormField";
import { useAuth } from "../contexts/AuthContext";
import type { LoginResponse, User } from "../types/api";

const loginSchema = z.object({ email: z.string().email("Enter a valid email address."), password: z.string().min(1, "Enter your password.") });
const registerSchema = loginSchema.extend({ fullName: z.string().trim().min(2, "Full name must be at least 2 characters.").max(100, "Full name must be 100 characters or fewer."), password: z.string().min(8, "Password must be at least 8 characters.").max(50, "Password must be 50 characters or fewer.") });
type LoginValues = z.infer<typeof loginSchema>;
type RegisterValues = z.infer<typeof registerSchema>;

export function LoginPage() {
  const { token, signIn } = useAuth();
  const navigate = useNavigate();
  const form = useForm<LoginValues>({ resolver: zodResolver(loginSchema), defaultValues: { email: "", password: "" } });
  const submit = async (values: LoginValues) => {
    form.clearErrors("root");
    try { const response = await unwrap<LoginResponse>(api.post("/api/auth/login", values)); signIn(response.token, response.user); navigate("/dashboard", { replace: true }); }
    catch (error) { form.setError("root", { message: errorMessage(error, "We could not sign you in. Check your details and try again.") }); }
  };
  if (token) return <Navigate to="/dashboard" replace/>;
  return <AuthFrame title="Welcome back" detail="Sign in to your personal finance workspace." alternate="New to FinanceFlow?" linkTo="/register" linkLabel="Create an account"><form className="form-grid form-offset" onSubmit={form.handleSubmit(submit)}><InputField id="email" label="Email" type="email" autoComplete="email" error={form.formState.errors.email?.message} {...form.register("email")}/><InputField id="password" label="Password" type="password" autoComplete="current-password" error={form.formState.errors.password?.message} {...form.register("password")}/>{form.formState.errors.root && <p className="field-error" role="alert">{form.formState.errors.root.message}</p>}<Button type="submit" disabled={form.formState.isSubmitting}>{form.formState.isSubmitting ? "Signing in…" : "Sign in"}</Button></form></AuthFrame>;
}

export function RegisterPage() {
  const { token } = useAuth();
  const navigate = useNavigate();
  const form = useForm<RegisterValues>({ resolver: zodResolver(registerSchema), defaultValues: { fullName: "", email: "", password: "" } });
  const submit = async (values: RegisterValues) => {
    form.clearErrors("root");
    try { await unwrap<User>(api.post("/api/auth/register", values)); navigate("/login", { replace: true }); }
    catch (error) { form.setError("root", { message: errorMessage(error, "We could not create your account. Try another email address.") }); }
  };
  if (token) return <Navigate to="/dashboard" replace/>;
  return <AuthFrame title="Create your account" detail="Start managing your money with clarity." alternate="Already have an account?" linkTo="/login" linkLabel="Sign in"><form className="form-grid form-offset" onSubmit={form.handleSubmit(submit)}><InputField id="fullName" label="Full name" autoComplete="name" error={form.formState.errors.fullName?.message} {...form.register("fullName")}/><InputField id="email" label="Email" type="email" autoComplete="email" error={form.formState.errors.email?.message} {...form.register("email")}/><InputField id="password" label="Password" type="password" autoComplete="new-password" error={form.formState.errors.password?.message} {...form.register("password")}/>{form.formState.errors.root && <p className="field-error" role="alert">{form.formState.errors.root.message}</p>}<Button type="submit" disabled={form.formState.isSubmitting}>{form.formState.isSubmitting ? "Creating account…" : "Create account"}</Button></form></AuthFrame>;
}

function AuthFrame({ title, detail, alternate, linkTo, linkLabel, children }: { title: string; detail: string; alternate: string; linkTo: string; linkLabel: string; children: React.ReactNode }) {
  return <main className="auth-shell"><section className="card auth-card" aria-labelledby="auth-title"><div className="brand auth-brand"><span className="brand-mark"><PiggyBank size={18}/></span>FinanceFlow</div><h1 id="auth-title">{title}</h1><p className="subtle">{detail}</p>{children}<p className="subtle action-offset">{alternate} <NavLink className="auth-link" to={linkTo}>{linkLabel}</NavLink></p></section></main>;
}
