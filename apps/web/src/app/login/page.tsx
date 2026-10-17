
"use client";

import { FormEvent, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { login } from "@/lib/api-client";


export default function LoginPage() {
    const router = useRouter();
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setError("");
        setLoading(true);

        try {
            const result = await login(email.trim(), password);

            if (!result.authenticated) {
                setError("Authentication was not confirmed. Please try again.");
                return;
            }

            router.replace("/scans/new");
        } catch (err) {
            setError(
                err instanceof Error
                    ? err.message
                    : "Unable to sign in. Check your API connection and try again.",
            );
        } finally {
            setLoading(false);
        }
    }

    return (
        <main className="auth-page">
            <section className="auth-card">
                <Link href="/" className="auth-brand">
                    <span className="auth-brand-icon">C</span>
                    <span>CloudTrap <span className="auth-brand-accent">AI</span></span>
                </Link>

                <div className="auth-eyebrow">SECURE ACCESS</div>
                <h1>Welcome back</h1>
                <p className="auth-description">
                    Sign in to manage your cloud security workspaces and scans.
                </p>

                {error && (
                    <div className="auth-error" role="alert">
                        {error}
                    </div>
                )}

                <form onSubmit={handleSubmit} className="auth-form">
                    <label htmlFor="email">Email address</label>
                    <input
                        id="email"
                        type="email"
                        autoComplete="email"
                        value={email}
                        onChange={(event) => setEmail(event.target.value)}
                        maxLength={320}
                        required
                        placeholder="you@example.com"
                    />

                    <label htmlFor="password">Password</label>
                    <input
                        id="password"
                        type="password"
                        autoComplete="current-password"
                        value={password}
                        onChange={(event) => setPassword(event.target.value)}
                        maxLength={1024}
                        required
                        placeholder="Enter your password"
                    />

                    <button type="submit" disabled={loading}>
                        {loading ? "Signing in..." : "Sign in"}
                    </button>
                </form>

                <p className="auth-footer">
                    New to CloudTrap AI?{" "}
                    <Link href="/register">Create an account</Link>
                </p>

                <p className="auth-security-note">
                    <span aria-hidden="true">â—†</span> Protected session-based access
                </p>
            </section>
        </main>
    );
}

