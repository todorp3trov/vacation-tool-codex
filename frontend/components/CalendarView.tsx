import { HolidayItem, VacationItem } from "../types/dashboard";

type Props = {
  rangeStart: string;
  myVacations: VacationItem[];
  teammateVacations: VacationItem[];
  holidays: HolidayItem[];
  loading?: boolean;
  onRangeChange?: (range: { start: string; end: string }) => void;
};

const statusClass = (status: string) => {
  switch (status) {
    case "APPROVED":
      return "pill approved";
    case "PENDING":
      return "pill pending";
    case "DENIED":
      return "pill denied";
    default:
      return "pill neutral";
  }
};

const toIso = (date: Date) => date.toISOString().split("T")[0];

export default function CalendarView({
  rangeStart,
  myVacations,
  teammateVacations,
  holidays,
  loading,
  onRangeChange
}: Props) {
  const startDate = new Date(rangeStart);
  const year = startDate.getFullYear();
  const month = startDate.getMonth();
  const monthLabel = startDate.toLocaleString("default", { month: "long", year: "numeric" });
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const firstDayIndex = new Date(year, month, 1).getDay();

  const handleNavigate = (delta: number) => {
    const newStart = new Date(year, month + delta, 1);
    const newEnd = new Date(newStart.getFullYear(), newStart.getMonth() + 1, 0);
    onRangeChange?.({ start: toIso(newStart), end: toIso(newEnd) });
  };

  const isWithinRange = (dayIso: string, start: string, end: string) => {
    return dayIso >= start && dayIso <= end;
  };

  const calendarDays: Array<{ day: number; iso: string } | null> = [];
  for (let i = 0; i < firstDayIndex; i += 1) {
    calendarDays.push(null);
  }
  for (let day = 1; day <= daysInMonth; day += 1) {
    const iso = toIso(new Date(year, month, day));
    calendarDays.push({ day, iso });
  }

  return (
    <div className="calendar-card">
      <div className="card-header">
        <div>
          <p className="eyebrow">Calendar</p>
          <h2 className="card-title">{monthLabel}</h2>
          <p className="muted">Navigate to load holidays and team vacations for other months.</p>
        </div>
        <div className="controls">
          <button className="ghost-btn" onClick={() => handleNavigate(-1)} aria-label="Previous month">
            ← Prev
          </button>
          <button className="ghost-btn" onClick={() => handleNavigate(1)} aria-label="Next month">
            Next →
          </button>
        </div>
      </div>
      {loading && <div className="loading-bar">Refreshing dashboard…</div>}
      <div className="calendar-grid">
        {["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"].map((label) => (
          <div key={label} className="calendar-head">
            {label}
          </div>
        ))}
        {calendarDays.map((cell, idx) => {
          if (!cell) {
            return <div key={`empty-${idx}`} className="calendar-cell empty" />;
          }
          const holidaysForDay = holidays.filter((h) => h.date === cell.iso);
          const myForDay = myVacations.filter((v) => isWithinRange(cell.iso, v.startDate, v.endDate));
          const teamForDay = teammateVacations.filter((v) => isWithinRange(cell.iso, v.startDate, v.endDate));
          return (
            <div key={cell.iso} className="calendar-cell">
              <div className="cell-header">
                <span className="day-number">{cell.day}</span>
                {holidaysForDay.map((h) => (
                  <span key={h.id} className="holiday-chip">
                    {h.name}
                  </span>
                ))}
              </div>
              {myForDay.length > 0 && (
                <div className="cell-row">
                  <span className="pill label">You</span>
                  <div className="pill-group">
                    {myForDay.map((v) => (
                      <span key={v.id} className={statusClass(v.status)}>
                        {v.status.toLowerCase()}
                      </span>
                    ))}
                  </div>
                </div>
              )}
              {teamForDay.length > 0 && (
                <div className="cell-row">
                  <span className="pill label">Team</span>
                  <div className="pill-group">
                    {teamForDay.map((v) => (
                      <span key={v.id} className={statusClass(v.status)}>
                        {v.employeeName}
                      </span>
                    ))}
                  </div>
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
