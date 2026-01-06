import { ManagerPendingItem } from "../types/dashboard";

type Props = {
  items: ManagerPendingItem[];
  selectedId?: string | null;
  onSelect: (id: string) => void;
};

export default function PendingRequestsList({ items, selectedId, onSelect }: Props) {
  return (
    <div className="list-card">
      <div className="card-header">
        <div>
          <p className="eyebrow">Manager</p>
          <h2 className="card-title">Pending requests</h2>
        </div>
      </div>
      {items.length === 0 ? (
        <p className="muted">No pending requests right now.</p>
      ) : (
        <ul className="vacation-list">
          {items.map((item) => {
            const isSelected = selectedId === item.requestId;
            return (
              <li
                key={item.requestId}
                className={`vacation-row ${isSelected ? "selected-row" : ""}`}
                onClick={() => onSelect(item.requestId)}
              >
                <div>
                  <p className="vacation-range">
                    {item.startDate} → {item.endDate}
                  </p>
                  <p className="muted">
                    {item.employeeName} · {item.numberOfDays} days
                  </p>
                </div>
                <div className="pill-group">
                  {item.balance?.unavailable ? (
                    <span className="pill pending">Balance unavailable</span>
                  ) : (
                    <span className="pill mine">
                      Tentative {item.balance?.tentativeBalance ?? "—"}
                    </span>
                  )}
                  <span className="pill pending">Pending</span>
                </div>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
