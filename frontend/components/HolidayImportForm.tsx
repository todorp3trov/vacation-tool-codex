import { useState } from "react";
import { HolidayImportSummary } from "../types/admin";

type Props = {
  onImported: (summary: HolidayImportSummary) => void;
  integrationReady: boolean;
};

const apiBase = (process.env.NEXT_PUBLIC_API_BASE ?? "http://localhost:8080").replace(/\/$/, "");

export default function HolidayImportForm({ onImported, integrationReady }: Props) {
  const currentYear = new Date().getFullYear();
  const [year, setYear] = useState<number>(currentYear);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const handleImport = async () => {
    setLoading(true);
    setError(null);
    setMessage(null);
    try {
      const response = await fetch(`${apiBase}/api/admin/holidays/import`, {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ year })
      });
      if (response.status === 401) {
        window.location.assign("/login");
        return;
      }
      const body = (await response.json()) as HolidayImportSummary;
      if (!response.ok) {
        setError(body.message ?? "Import failed.");
        return;
      }
      setMessage(body.message ?? "Import complete.");
      onImported(body);
    } catch (err) {
      setError("Unable to import holidays right now.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="summary-card">
      <div className="card-header">
        <div>
          <p className="eyebrow">Holiday API</p>
          <h3 className="card-title">Import holidays</h3>
        </div>
        <span className={`pill ${integrationReady ? "approved" : "denied"}`}>{integrationReady ? "Ready" : "Disabled"}</span>
      </div>
      <div className="form-inline">
        <label className="form-field">
          <span className="label">Year</span>
          <input
            type="number"
            min={1950}
            max={2100}
            value={year}
            onChange={(e) => setYear(parseInt(e.target.value, 10))}
            disabled={loading || !integrationReady}
          />
        </label>
        <button className="primary-btn" onClick={handleImport} disabled={loading || !integrationReady}>
          {loading ? "Importing…" : "Run import"}
        </button>
      </div>
      {error && <div className="alert alert-error">{error}</div>}
      {message && <div className="alert alert-warning">{message}</div>}
    </div>
  );
}
