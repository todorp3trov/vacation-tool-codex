import { FormEvent, useState } from "react";

type LoginResponse = {
  homeRoute: string;
};

type Props = {
  onLoginSuccess?: (homeRoute: string) => void;
};

export default function LoginForm({ onLoginSuccess }: Props) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const apiBase = (process.env.NEXT_PUBLIC_API_BASE ?? "").replace(/\/$/, "");

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (isSubmitting) {
      return;
    }

    if (!username.trim() || !password.trim()) {
      setError("Username and password are required");
      return;
    }

    setIsSubmitting(true);
    setError(null);
    try {
      const response = await fetch(`${apiBase}/api/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify({ username, password })
      });

      if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        setError(body.error ?? "Invalid username or password");
        setIsSubmitting(false);
        return;
      }

      const data = (await response.json()) as LoginResponse;
      if (onLoginSuccess) {
        onLoginSuccess(data.homeRoute);
      } else {
        window.location.assign(data.homeRoute || "/");
      }
    } catch (err) {
      setError("Unable to log in right now. Please try again.");
      setIsSubmitting(false);
    }
  };

  return (
    <form className="login-form" onSubmit={handleSubmit} noValidate>
      <h1 className="login-title">Vacation Tool</h1>
      <label className="login-label">
        Username
        <input
          type="text"
          name="username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          autoComplete="username"
          required
        />
      </label>
      <label className="login-label">
        Password
        <input
          type="password"
          name="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          autoComplete="current-password"
          required
        />
      </label>
      {error && <p className="login-error">{error}</p>}
      <button type="submit" disabled={isSubmitting}>
        {isSubmitting ? "Signing in..." : "Sign in"}
      </button>
    </form>
  );
}
