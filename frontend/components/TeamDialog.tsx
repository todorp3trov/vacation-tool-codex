import { useEffect, useMemo, useState } from "react";
import { AdminTeam, AdminUser } from "../types/admin";

type Props = {
  open: boolean;
  onClose: () => void;
  onSaved: (team: AdminTeam) => void;
  users: AdminUser[];
  team?: AdminTeam | null;
};

const apiBase = (process.env.NEXT_PUBLIC_API_BASE ?? "http://localhost:8080").replace(/\/$/, "");

export default function TeamDialog({ open, onClose, onSaved, users, team }: Props) {
  const [name, setName] = useState("");
  const [status, setStatus] = useState("ACTIVE");
  const [members, setMembers] = useState<string[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (team) {
      setName(team.name);
      setStatus(team.status);
      setMembers(team.memberIds ?? []);
    } else {
      setName("");
      setStatus("ACTIVE");
      setMembers([]);
    }
    setError(null);
  }, [team, open]);

  const heading = useMemo(() => (team ? "Edit team" : "Create team"), [team]);

  const toggleMember = (userId: string) => {
    setMembers((prev) => {
      if (prev.includes(userId)) {
        return prev.filter((id) => id !== userId);
      }
      return [...prev, userId];
    });
  };

  const handleSubmit = async () => {
    setLoading(true);
    setError(null);
    const payload = { name, status, memberIds: members };
    try {
      const url = team ? `${apiBase}/api/admin/teams/${team.id}` : `${apiBase}/api/admin/teams`;
      const response = await fetch(url, {
        method: team ? "PUT" : "POST",
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
        setError(body.error ?? "Unable to save team.");
        return;
      }
      onSaved(body as AdminTeam);
      onClose();
    } catch {
      setError("Unable to save team.");
    } finally {
      setLoading(false);
    }
  };

  if (!open) return null;

  return (
    <div className="dialog-backdrop">
      <div className="dialog" style={{ width: "480px" }}>
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
            <span className="label">Team name</span>
            <input type="text" value={name} onChange={(e) => setName(e.target.value)} disabled={loading} />
          </label>

          <label className="form-field">
            <span className="label">Status</span>
            <select value={status} onChange={(e) => setStatus(e.target.value)} disabled={loading}>
              <option value="ACTIVE">Active</option>
              <option value="ARCHIVED">Archived</option>
            </select>
            {status === "ARCHIVED" && <span className="muted small">Archived teams cannot receive new members.</span>}
          </label>

          <div className="form-field">
            <span className="label">Members</span>
            <div className="pill-row">
              {users.map((u) => (
                <label key={u.id} className="pill checkbox-pill">
                  <input
                    type="checkbox"
                    checked={members.includes(u.id)}
                    onChange={() => toggleMember(u.id)}
                    disabled={loading || status === "ARCHIVED"}
                  />
                  {u.displayName} <span className="muted small">({u.username})</span>
                </label>
              ))}
            </div>
          </div>
        </div>

        <div className="dialog-actions">
          <button className="ghost-btn" onClick={onClose} disabled={loading}>
            Cancel
          </button>
          <button className="primary-btn" onClick={handleSubmit} disabled={loading || !name.trim()}>
            {loading ? "Saving…" : team ? "Save changes" : "Create team"}
          </button>
        </div>
      </div>
    </div>
  );
}
