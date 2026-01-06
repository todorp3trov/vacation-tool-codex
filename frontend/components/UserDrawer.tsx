import { useEffect, useMemo, useState } from "react";
import { AdminTeam, AdminUser } from "../types/admin";

type Props = {
  open: boolean;
  onClose: () => void;
  onSaved: (user: AdminUser) => void;
  teams: AdminTeam[];
  roles: string[];
  user?: AdminUser | null;
};

const apiBase = (process.env.NEXT_PUBLIC_API_BASE ?? "http://localhost:8080").replace(/\/$/, "");

export default function UserDrawer({ open, onClose, onSaved, teams, roles, user }: Props) {
  const [username, setUsername] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [password, setPassword] = useState("");
  const [status, setStatus] = useState("ACTIVE");
  const [selectedRoles, setSelectedRoles] = useState<string[]>([]);
  const [teamIds, setTeamIds] = useState<string[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [checking, setChecking] = useState(false);
  const [usernameAvailable, setUsernameAvailable] = useState<boolean | null>(null);

  useEffect(() => {
    if (user) {
      setUsername(user.username);
      setDisplayName(user.displayName);
      setStatus(user.status);
      setSelectedRoles(user.roles ?? []);
      setTeamIds(user.teamIds ?? []);
      setPassword("");
      setError(null);
      setUsernameAvailable(true);
    } else {
      setUsername("");
      setDisplayName("");
      setStatus("ACTIVE");
      setSelectedRoles(["EMPLOYEE"]);
      setTeamIds([]);
      setPassword("");
      setError(null);
      setUsernameAvailable(null);
    }
  }, [user, open]);

  const heading = useMemo(() => (user ? "Edit user" : "Invite new user"), [user]);

  const toggleRole = (code: string) => {
    setSelectedRoles((prev) => {
      if (prev.includes(code)) {
        return prev.filter((r) => r !== code);
      }
      return [...prev, code];
    });
  };

  const toggleTeam = (teamId: string) => {
    setTeamIds((prev) => {
      if (prev.includes(teamId)) {
        return prev.filter((id) => id !== teamId);
      }
      return [...prev, teamId];
    });
  };

  const checkUsername = async (value: string) => {
    if (!value || (user && value.toLowerCase() === user.username.toLowerCase())) {
      setUsernameAvailable(true);
      return;
    }
    setChecking(true);
    try {
      const response = await fetch(`${apiBase}/api/admin/users/check-username?username=${encodeURIComponent(value)}`, {
        credentials: "include"
      });
      if (response.status === 401) {
        window.location.assign("/login");
        return;
      }
      const body = await response.json();
      setUsernameAvailable(Boolean(body.available));
    } catch {
      setUsernameAvailable(null);
    } finally {
      setChecking(false);
    }
  };

  const handleSubmit = async () => {
    setError(null);
    setLoading(true);
    if (!username.trim() || !displayName.trim()) {
      setError("Username and display name are required.");
      setLoading(false);
      return;
    }
    if (!user && !password) {
      setError("Password is required.");
      setLoading(false);
      return;
    }
    if (selectedRoles.length === 0) {
      setError("Select at least one role.");
      setLoading(false);
      return;
    }
    if (usernameAvailable === false) {
      setError("Username already taken.");
      setLoading(false);
      return;
    }
    const payload = {
      username,
      displayName,
      password: password || undefined,
      status,
      roles: selectedRoles,
      teamIds
    };
    try {
      const url = user ? `${apiBase}/api/admin/users/${user.id}` : `${apiBase}/api/admin/users`;
      const response = await fetch(url, {
        method: user ? "PUT" : "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
      });
      if (response.status === 401) {
        window.location.assign("/login");
        return;
      }
      const body = await response.json().catch(() => ({}));
      if (!response.ok) {
        setError(body.error ?? "Unable to save user.");
        return;
      }
      onSaved(body as AdminUser);
      onClose();
    } catch (err) {
      setError("Unable to save user.");
    } finally {
      setLoading(false);
    }
  };

  if (!open) return null;

  return (
    <div className="dialog-backdrop">
      <div className="dialog" style={{ width: "520px" }}>
        <div className="dialog-header">
          <div>
            <p className="eyebrow">Admin</p>
            <h3 className="page-title">{heading}</h3>
          </div>
          <button className="ghost-btn" onClick={onClose}>
            Close
          </button>
        </div>

        {error && <div className="alert alert-error">{error}</div>}

        <div className="dialog-body" style={{ gridTemplateColumns: "1fr" }}>
          <label className="form-field">
            <span className="label">Username</span>
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              onBlur={(e) => checkUsername(e.target.value)}
              disabled={loading}
            />
            {checking && <span className="muted small">Checking availability…</span>}
            {username && usernameAvailable === false && <span className="alert alert-warning">Username already taken</span>}
          </label>

          <label className="form-field">
            <span className="label">Display name</span>
            <input type="text" value={displayName} onChange={(e) => setDisplayName(e.target.value)} disabled={loading} />
          </label>

          <label className="form-field">
            <span className="label">{user ? "Reset password (optional)" : "Password"}</span>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder={user ? "Leave blank to keep existing password" : ""}
              disabled={loading}
            />
          </label>

          <label className="form-field">
            <span className="label">Status</span>
            <select value={status} onChange={(e) => setStatus(e.target.value)} disabled={loading}>
              <option value="ACTIVE">Active</option>
              <option value="DISABLED">Disabled</option>
            </select>
          </label>

          <div className="form-field">
            <span className="label">Roles</span>
            <div className="pill-row">
              {roles.map((role) => (
                <label key={role} className="pill checkbox-pill">
                  <input
                    type="checkbox"
                    checked={selectedRoles.includes(role)}
                    onChange={() => toggleRole(role)}
                    disabled={loading}
                  />
                  {role}
                </label>
              ))}
            </div>
          </div>

          <div className="form-field">
            <span className="label">Teams</span>
            {teams.length === 0 ? (
              <p className="muted small">No teams configured yet.</p>
            ) : (
              <div className="pill-row">
                {teams.map((team) => (
                  <label key={team.id} className="pill checkbox-pill">
                    <input
                      type="checkbox"
                      checked={teamIds.includes(team.id)}
                      onChange={() => toggleTeam(team.id)}
                      disabled={loading || team.status === "ARCHIVED"}
                    />
                    {team.name}
                    {team.status === "ARCHIVED" && <span className="muted small"> (archived)</span>}
                  </label>
                ))}
              </div>
            )}
          </div>
        </div>

        <div className="dialog-actions">
          <button className="ghost-btn" onClick={onClose} disabled={loading}>
            Cancel
          </button>
          <button className="primary-btn" onClick={handleSubmit} disabled={loading || usernameAvailable === false}>
            {loading ? "Saving…" : user ? "Save changes" : "Create user"}
          </button>
        </div>
      </div>
    </div>
  );
}
