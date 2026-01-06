import { useEffect, useState } from "react";
import { HrProcessingDetail } from "../types/dashboard";

type Props = {
  detail: HrProcessingDetail | null;
  loading?: boolean;
  error?: string | null;
  successMessage?: string | null;
  onProcess: (hrNotes: string) => void;
};

export default function RequestProcessingPanel({ detail, loading, error, successMessage, onProcess }: Props) {
  const [notes, setNotes] = useState("");

  useEffect(() => {
    if (detail?.request?.hrNotes) {
      setNotes(detail.request.hrNotes);
    } else {
      setNotes("");
    }
  }, [detail?.request.requestId, detail?.request.hrNotes]);

  if (!detail) {
    return (
      <div className="list-card">
        <div className="card-header">
          <div>
            <p className="eyebrow">Processing</p>
            <h2 className="card-title">Select a request</h2>
          </div>
        </div>
        <p className="muted">Choose an approved request to review and process.</p>
      </div>
    );
  }

  const request = detail.request;

  return (
    <div className="list-card">
      <div className="card-header">
        <div>
          <p className="eyebrow">Processing</p>
          <h2 className="card-title">Request detail</h2>
          <p className="muted">
            {request.employeeName} · {request.startDate} → {request.endDate} ({request.numberOfDays} days)
          </p>
        </div>
      </div>

      {error && <div className="alert alert-error">{error}</div>}
      {successMessage && <div className="alert alert-success">{successMessage}</div>}
      {loading && <div className="loading-bar">Processing…</div>}

      <div className="decision-grid">
        <div>
          <p className="muted">Request code</p>
          <p className="muted">{request.requestCode ?? "—"}</p>
        </div>
        <div>
          <p className="muted">Manager note</p>
          <p className="muted">{request.managerNotes?.length ? request.managerNotes : "No note"}</p>
        </div>
        <div>
          <p className="muted">Current status</p>
          <p className="pill approved">{request.status}</p>
        </div>
      </div>

      <label className="decision-note">
        HR notes (stored)
        <textarea value={notes} onChange={(e) => setNotes(e.target.value)} rows={3} />
      </label>

      <div className="dialog-actions">
        <button className="primary-btn" onClick={() => onProcess(notes)} disabled={loading}>
          Mark as Processed
        </button>
      </div>
    </div>
  );
}
