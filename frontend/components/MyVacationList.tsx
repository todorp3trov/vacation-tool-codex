import { VacationItem } from "../types/dashboard";

type Props = {
  vacations: VacationItem[];
  title?: string;
};

const formatRange = (start: string, end: string) => {
  const startDate = new Date(start);
  const endDate = new Date(end);
  const formatter = new Intl.DateTimeFormat("default", { month: "short", day: "numeric" });
  return `${formatter.format(startDate)} - ${formatter.format(endDate)}`;
};

export default function MyVacationList({ vacations, title = "My Vacation Requests" }: Props) {
  return (
    <div className="list-card">
      <div className="card-header">
        <div>
          <p className="eyebrow">History</p>
          <h2 className="card-title">{title}</h2>
        </div>
      </div>
      {vacations.length === 0 ? (
        <p className="muted">No requests yet. Plan ahead and request time off.</p>
      ) : (
        <ul className="vacation-list">
          {vacations.map((vacation) => (
            <li key={vacation.id} className="vacation-row">
              <div>
                <p className="vacation-range">{formatRange(vacation.startDate, vacation.endDate)}</p>
                <p className="muted">{vacation.employeeName}</p>
              </div>
              <span className={`pill ${vacation.mine ? "mine" : "team"}`}>{vacation.status}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
