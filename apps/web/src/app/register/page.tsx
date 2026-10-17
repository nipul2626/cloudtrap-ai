
"use client";

import { FormEvent, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { register } from "@/lib/api-client";


export default function RegisterPage() {
    const router = useRouter();
    const [displayName, setDisplayName] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setError("");

        if (password.length < 12) {
            setError("Your password must contain at least 12 characters.");
            return;
        }

        if (password !== confirmPassword) {
            setError("The passwords do not match.");
            return;
        }

        setLoading(true);

        try {
            await register(email.trim(), password, displayName.trim());
            router.replace("/login?registered=1");
        } catch (err) {
            setError(
                err instanceof Error
                    ? err.message
                    : "Unable to create your account. Please try again.",
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

                <div className="auth-eyebrow">GET STARTED</div>
                <h1>Create your account</h1>
                <p className="auth-description">
                    Set up your account to start exploring cloud security findings.
                </p>

                {error && (
                    <div className="auth-error" role="alert">
                        {error}
                    </div>
                )}

                <form onSubmit={handleSubmit} className="auth-form">
                    <label htmlFor="displayName">Display name</label>
                    <input
                        id="displayName"
                        type="text"
                        autoComplete="name"
                        value={displayName}
                        onChange={(event) => setDisplayName(event.target.value)}
                        maxLength={120}
                        required
                        placeholder="Your name"
                    />

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
                        autoComplete="new-password"
                        value={password}
                        onChange={(event) => setPassword(event.target.value)}
                        minLength={12}
                        maxLength={1024}
                        required
                        placeholder="At least 12 characters"
                    />

                    <label htmlFor="confirmPassword">Confirm password</label>
                    <input
                        id="confirmPassword"
                        type="password"
                        autoComplete="new-password"
                        value={confirmPassword}
                        onChange={(event) => setConfirmPassword(event.target.value)}
                        minLength={12}
                        maxLength={1024}
                        required
                        placeholder="Enter your password again"
                    />

                    <button type="submit" disabled={loading}>
                        {loading ? "Creating account..." : "Create account"}
                    </button>
                </form>

                <p className="auth-footer">
                    Already have an account?{" "}
                    <Link href="/login">Sign in</Link>
                </p>

                <p className="auth-security-note">
                    <span aria-hidden="true">â—†</span> Secure session-based access
                </p>
            </section>
        </main>
    );
}

