import Head from "next/head";
import { GetServerSideProps } from "next";
import { useState } from "react";
import BalanceSummary from "../../components/BalanceSummary";
import CalendarView from "../../components/CalendarView";
import MyVacationList from "../../components/MyVacationList";
import { DashboardPayload } from "../../types/dashboard";

type Range = {
  start: string;
  end: string;
};

type Props = {
  initialData: DashboardPayload | null;
  initialRange: Range;
  error?: string | null;
};

const apiBase = (process.env.NEXT_PUBLIC_API_BASE ?? "http://localhost:8080").replace(/\/$/, "");

const formatRangeLabel = (range: Range) => {
  const formatter = new Intl.DateTimeFormat("default", { month: "short", day: "numeric" });
  return `${formatter.format(new Date(range.start))} to ${formatter.format(new Date(range.end))}`;
};

export async function fetchDashboardData(range: Range, cookie?: string): Promise<DashboardPayload> {
  const base = apiBase || "";
  const url = `${base}/api/employee/dashboard?start=${range.start}&end=${range.end}`;
  const response = await fetch(url, {
    credentials: "include",
    headers: cookie ? { cookie } : undefined
  });
  if (response.status === 401) {
    throw new Error("unauthorized");
  }
  if (!response.ok) {
    throw new Error("dashboard_load_failed");
  }
  return (await response.json()) as DashboardPayload;
}

const defaultRange = (): Range => {
  const now = new Date();
  const start = new Date(now.getFullYear(), now.getMonth(), 1);
  const end = new Date(now.getFullYear(), now.getMonth() + 1, 0);
  return {
    start: start.toISOString().split("T")[0],
    end: end.toISOString().split("T")[0]
  };
};

export const getServerSideProps: GetServerSideProps<Props> = async (context) => {
  const range = defaultRange();
  try {
    const data = await fetchDashboardData(range, context.req.headers.cookie ?? undefined);
    return { props: { initialData: data, initialRange: range, error: null } };
  } catch (error: any) {
    if (error?.message === "unauthorized") {
      return {
        redirect: {
          destination: "/login",
          permanent: false
        }
      };
    }
    return { props: { initialData: null, initialRange: range, error: "Unable to load dashboard right now." } };
  }
};

export default function DashboardPage({ initialData, initialRange, error: initialError }: Props) {
  const [data, setData] = useState<DashboardPayload | null>(initialData);
  const [range, setRange] = useState<Range>(initialRange);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(initialError ?? null);

  const refreshRange = async (nextRange: Range) => {
    setRange(nextRange);
    setLoading(true);
    setError(null);
    try {
      const payload = await fetchDashboardData(nextRange);
      setData(payload);
    } catch (err: any) {
      if (err?.message === "unauthorized") {
        window.location.assign("/login");
        return;
      }
      setError("Unable to refresh dashboard for that range.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <Head>
        <title>Vacation Tool | Dashboard</title>
      </Head>
      <div className="dashboard-shell">
        <header className="dashboard-header">
          <div>
            <p className="eyebrow">Employee</p>
            <h1 className="page-title">Vacation Dashboard</h1>
            <p className="muted">Read-only overview of your balance, upcoming vacations, and team visibility.</p>
          </div>
          <div className="range-chip">
            <span className="pill neutral">Range</span>
            <strong>{formatRangeLabel(range)}</strong>
          </div>
        </header>

        {error && <div className="alert alert-error">{error}</div>}

        {data && (
          <div className="dashboard-grid">
            <BalanceSummary
              officialBalance={data.balance.officialBalance}
              tentativeBalance={data.balance.tentativeBalance}
              unavailable={data.balance.unavailable}
              message={data.balance.message ?? undefined}
            />
            <div className="team-card">
              <p className="eyebrow">Team snapshot</p>
              <h2 className="card-title">Upcoming team vacations</h2>
              {data.teammateVacations.length === 0 ? (
                <p className="muted">No team vacations in this range.</p>
              ) : (
                <ul className="team-list">
                  {data.teammateVacations.slice(0, 5).map((vacation) => (
                    <li key={vacation.id} className="team-row">
                      <div>
                        <strong>{vacation.employeeName}</strong>
                        <p className="muted">
                          {vacation.startDate} → {vacation.endDate}
                        </p>
                      </div>
                      <span className="pill team">{vacation.status}</span>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>
        )}

        <div className="dashboard-grid">
          {data && (
            <CalendarView
              rangeStart={range.start}
              myVacations={data.myVacations}
              teammateVacations={data.teammateVacations}
              holidays={data.holidays}
              loading={loading}
              onRangeChange={refreshRange}
            />
          )}
          <MyVacationList vacations={data?.myVacations ?? []} />
        </div>
      </div>
    </>
  );
}
