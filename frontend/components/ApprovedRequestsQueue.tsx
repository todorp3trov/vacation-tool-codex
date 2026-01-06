import { HrProcessingItem } from "../types/dashboard";

type Props = {
  items: HrProcessingItem[];
  selectedId?: string | null;
  onSelect: (id: string) => void;
};

export default function ApprovedRequestsQueue({ items, selectedId, onSelect }: Props) {
  return (
    <div className="list-card">
      <div className="card-header">
        <div>
          <p className="eyebrow">HR</p>
          <h2 className="card-title">Approved requests</h2>
          <p className="muted">Ready for processing</p>
        </div>
      </div>
      {items.length === 0 ? (
        <p className="muted">No approved requests need processing.</p>
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
                    {item.employeeName} · {item.numberOfDays} days · {item.requestCode ?? "No code"}
                  </p>
                </div>
                <div className="pill-group">
                  <span className="pill approved">Approved</span>
                  <span className="pill neutral">Unprocessed</span>
                </div>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
