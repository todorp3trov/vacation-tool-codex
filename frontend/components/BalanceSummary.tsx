type BalanceProps = {
  officialBalance: number | null;
  tentativeBalance: number | null;
  unavailable: boolean;
  message?: string | null;
};

export default function BalanceSummary({ officialBalance, tentativeBalance, unavailable, message }: BalanceProps) {
  return (
    <div className="summary-card">
      <div className="summary-header">
        <div>
          <p className="eyebrow">Vacation Balance</p>
          <h2 className="summary-title">Your remaining days</h2>
        </div>
        <span className={`status-chip ${unavailable ? "status-error" : "status-ok"}`}>
          {unavailable ? "Unavailable" : "Official"}
        </span>
      </div>
      {unavailable ? (
        <p className="summary-error">
          {message || "We can't reach the balance system right now. Please try again later."}
        </p>
      ) : (
        <div className="balance-grid">
          <div className="balance-box primary">
            <p className="label">Official balance</p>
            <p className="value">{officialBalance ?? "—"}</p>
          </div>
          <div className="balance-box muted">
            <p className="label">Tentative after pending</p>
            <p className="value">{tentativeBalance ?? "—"}</p>
          </div>
        </div>
      )}
    </div>
  );
}
