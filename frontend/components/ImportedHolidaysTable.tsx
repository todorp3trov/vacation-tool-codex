import { HolidayAdminItem } from "../types/admin";

type Props = {
  years: number[];
  selectedYear: number | null;
  holidays: HolidayAdminItem[];
  onSelectYear: (year: number) => void;
  onDeprecate: (id: string) => void;
};

export default function ImportedHolidaysTable({ years, selectedYear, holidays, onSelectYear, onDeprecate }: Props) {
  return (
    <div className="summary-card">
      <div className="card-header">
        <div>
          <p className="eyebrow">Imported holidays</p>
          <h3 className="card-title">Records by year</h3>
        </div>
        <select
          value={selectedYear ?? ""}
          onChange={(e) => onSelectYear(parseInt(e.target.value, 10))}
          disabled={years.length === 0}
          className="select-inline"
        >
          {years.map((year) => (
            <option key={year} value={year}>
              {year}
            </option>
          ))}
        </select>
      </div>

      {years.length === 0 ? (
        <p className="muted">No imported years yet.</p>
      ) : holidays.length === 0 ? (
        <p className="muted">No holidays for {selectedYear}.</p>
      ) : (
        <div className="table">
          <div className="table-head">
            <span>Date</span>
            <span>Name</span>
            <span>Status</span>
            <span></span>
          </div>
          {holidays.map((holiday) => (
            <div key={holiday.id} className="table-row">
              <span>{holiday.date}</span>
              <span>{holiday.name}</span>
              <span className={`pill ${holiday.status === "IMPORTED" ? "approved" : "denied"}`}>{holiday.status}</span>
              <div className="table-actions">
                {holiday.status === "IMPORTED" ? (
                  <button className="ghost-btn" onClick={() => onDeprecate(holiday.id)}>
                    Deprecate
                  </button>
                ) : (
                  <span className="muted small">{holiday.deprecationReason ?? "Deprecated"}</span>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
