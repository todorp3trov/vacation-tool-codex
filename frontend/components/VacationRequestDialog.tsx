import { useEffect, useMemo, useState } from "react";
import { BalanceSummary, ComputeDaysResponse, SubmissionResponse } from "../types/dashboard";

type DraftRange = { start: string; end: string };

type Props = {
  open: boolean;
  draftRange: DraftRange | null;
  balance: BalanceSummary | null;
  onClose: () => void;
  onSubmitted: () => Promise<void> | void;
};

const apiBase = (process.env.NEXT_PUBLIC_API_BASE ?? "").replace(/\/$/, "");

export default function VacationRequestDialog({ open, draftRange, balance, onClose, onSubmitted }: Props) {
  const [numberOfDays, setNumberOfDays] = useState<number | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const noticeError = useMemo(() => {
    if (!draftRange) return null;
    const start = new Date(draftRange.start);
    const now = new Date();
    const msDiff = start.getTime() - now.getTime();
    const daysDiff = Math.floor(msDiff / (1000 * 60 * 60 * 24));
    return daysDiff < 14 ? "Requests must be at least 14 days in advance." : null;
  }, [draftRange]);

  useEffect(() => {
    if (!open || !draftRange) {
      setNumberOfDays(null);
      setError(null);
      setSubmitting(false);
      return;
    }
    const fetchDays = async () => {
      setLoading(true);
      setError(null);
      try {
        const response = await fetch(
          `${apiBase}/api/compute-days?start=${draftRange.start}&end=${draftRange.end}`,
          { credentials: "include" }
        );
        if (!response.ok) {
          const body = await response.json().catch(() => ({}));
          setError(body.error ?? "Unable to compute days for that range.");
          setNumberOfDays(null);
          return;
        }
        const body = (await response.json()) as ComputeDaysResponse;
        setNumberOfDays(body.number_of_days);
      } catch (err) {
        setError("Unable to compute vacation days right now.");
        setNumberOfDays(null);
      } finally {
        setLoading(false);
      }
    };
    fetchDays();
  }, [draftRange, open]);

  const canSubmit =
    open &&
    draftRange &&
    !loading &&
    !submitting &&
    !error &&
    !noticeError &&
    numberOfDays !== null &&
    numberOfDays > 0 &&
    balance &&
    !balance.unavailable;

  const handleSubmit = async () => {
    if (!draftRange) return;
    setSubmitting(true);
    setError(null);
    try {
      const response = await fetch(`${apiBase}/api/vacation/submit`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify({ startDate: draftRange.start, endDate: draftRange.end })
      });
      if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        setError(body.message ?? body.error ?? "Unable to submit request.");
        setSubmitting(false);
        return;
      }
      await response.json().catch(() => ({} as SubmissionResponse));
      await onSubmitted();
      onClose();
    } catch (err) {
      setError("Submission failed. Please try again.");
      setSubmitting(false);
    }
  };

  if (!open || !draftRange) {
    return null;
  }

  return (
    <div className="dialog-backdrop">
      <div className="dialog">
        <div className="dialog-header">
          <div>
            <p className="eyebrow">New Request</p>
            <h3>Confirm your vacation request</h3>
            <p className="muted">
              Start {draftRange.start} → End {draftRange.end}
            </p>
          </div>
          <button className="ghost-btn" onClick={onClose} aria-label="Close request dialog">
            ✕
          </button>
        </div>

        {loading && <div className="loading-bar">Calculating working days…</div>}
        {error && <div className="alert alert-error">{error}</div>}
        {noticeError && <div className="alert alert-warning">{noticeError}</div>}

        <div className="dialog-body">
          <div className="stat">
            <p className="muted">Working days (excluding holidays)</p>
            <strong className="stat-value">{numberOfDays ?? "—"}</strong>
          </div>
          <div className="stat">
            <p className="muted">Official balance</p>
            <strong className="stat-value">{balance?.officialBalance ?? "—"}</strong>
          </div>
          <div className="stat">
            <p className="muted">Tentative after pending</p>
            <strong className="stat-value">{balance?.tentativeBalance ?? "—"}</strong>
          </div>
          {balance?.unavailable && <p className="muted small">Balance unavailable. Submission is blocked.</p>}
        </div>

        <div className="dialog-actions">
          <button className="ghost-btn" onClick={onClose} disabled={submitting}>
            Cancel
          </button>
          <button className="primary-btn" onClick={handleSubmit} disabled={!canSubmit}>
            {submitting ? "Submitting..." : "Submit request"}
          </button>
        </div>
      </div>
    </div>
  );
}
