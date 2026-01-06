import { useEffect, useState } from "react";
import { ManagerRequestDetail } from "../types/dashboard";

type Props = {
  detail: ManagerRequestDetail | null;
  loading?: boolean;
  error?: string | null;
  onApprove: (note: string) => void;
  onDeny: (note: string) => void;
};

export default function RequestDecisionPanel({ detail, loading, error, onApprove, onDeny }: Props) {
  const [note, setNote] = useState("");

  useEffect(() => {
    setNote("");
  }, [detail?.request.requestId]);

  if (!detail) {
    return (
      <div className="list-card">
        <div className="card-header">
          <div>
            <p className="eyebrow">Decision</p>
            <h2 className="card-title">Select a request</h2>
          </div>
        </div>
        <p className="muted">Choose a pending request to review overlaps and decide.</p>
      </div>
    );
  }

  const request = detail.request;

  return (
    <div className="list-card">
      <div className="card-header">
        <div>
          <p className="eyebrow">Decision</p>
          <h2 className="card-title">Request detail</h2>
          <p className="muted">
            {request.employeeName} · {request.startDate} → {request.endDate} ({request.numberOfDays} days)
          </p>
        </div>
      </div>

      {error && <div className="alert alert-error">{error}</div>}
      {loading && <div className="loading-bar">Processing…</div>}

      <div className="decision-grid">
        <div>
          <p className="muted">Balance</p>
          {request.balance?.unavailable ? (
            <p className="muted">Balance unavailable.</p>
          ) : (
            <p className="muted">
              Official {request.balance?.officialBalance ?? "—"} · Tentative {request.balance?.tentativeBalance ?? "—"}
            </p>
          )}
        </div>
        <div>
          <p className="muted">Holidays in range</p>
          {detail.holidays.length === 0 ? (
            <p className="muted">None</p>
          ) : (
            <ul className="mini-list">
              {detail.holidays.map((h) => (
                <li key={h.id}>
                  {h.date}: {h.name}
                </li>
              ))}
            </ul>
          )}
        </div>
        <div>
          <p className="muted">Overlaps</p>
          {detail.overlaps.length === 0 ? (
            <p className="muted">No overlaps in this range.</p>
          ) : (
            <ul className="mini-list">
              {detail.overlaps.map((ov) => (
                <li key={ov.id}>
                  {ov.employeeName} · {ov.startDate} → {ov.endDate} ({ov.status})
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>

      <label className="decision-note">
        Manager note (optional)
        <textarea value={note} onChange={(e) => setNote(e.target.value)} rows={3} />
      </label>

      <div className="dialog-actions">
        <button className="ghost-btn" onClick={() => onDeny(note)} disabled={loading}>
          Deny
        </button>
        <button className="primary-btn" onClick={() => onApprove(note)} disabled={loading}>
          Approve
        </button>
      </div>
    </div>
  );
}
