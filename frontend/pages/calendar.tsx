import Head from "next/head";
import { GetServerSideProps } from "next";
import { useEffect, useMemo, useState } from "react";
import BalanceSummary from "../components/BalanceSummary";
import CalendarView from "../components/CalendarView";
import MyVacationList from "../components/MyVacationList";
import PendingRequestsList from "../components/PendingRequestsList";
import RequestDecisionPanel from "../components/RequestDecisionPanel";
import VacationRequestDialog from "../components/VacationRequestDialog";
import { CalendarPayload, ManagerRequestDetail } from "../types/dashboard";

type Range = { start: string; end: string };

type Props = {
  initialData: CalendarPayload | null;
  initialRange: Range;
  error?: string | null;
};

const apiBase = (process.env.NEXT_PUBLIC_API_BASE ?? "http://localhost:8080").replace(/\/$/, "");

const formatRangeLabel = (range: Range) => {
  const formatter = new Intl.DateTimeFormat("default", { month: "short", day: "numeric" });
  return `${formatter.format(new Date(range.start))} to ${formatter.format(new Date(range.end))}`;
};

const formatDate = (date: Date) => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
};

const defaultRange = (): Range => {
  const now = new Date();
  const start = new Date(now.getFullYear(), now.getMonth(), 1);
  const end = new Date(now.getFullYear(), now.getMonth() + 1, 0);
  return {
    start: formatDate(start),
    end: formatDate(end)
  };
};

export async function fetchCalendarData(range: Range, cookie?: string): Promise<CalendarPayload> {
  const url = `${apiBase}/api/calendar?start=${range.start}&end=${range.end}`;
  const response = await fetch(url, {
    credentials: "include",
    headers: cookie ? { cookie } : undefined
  });
  if (response.status === 401) {
    throw new Error("unauthorized");
  }
  if (!response.ok) {
    throw new Error("calendar_load_failed");
  }
  return (await response.json()) as CalendarPayload;
}

export const getServerSideProps: GetServerSideProps<Props> = async (context) => {
  const range = defaultRange();
  try {
    const data = await fetchCalendarData(range, context.req.headers.cookie ?? undefined);
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
    return { props: { initialData: null, initialRange: range, error: "Unable to load calendar right now." } };
  }
};

export default function CalendarPage({ initialData, initialRange, error: initialError }: Props) {
  const [data, setData] = useState<CalendarPayload | null>(initialData);
  const [range, setRange] = useState<Range>(initialRange);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(initialError ?? null);
  const [draftRange, setDraftRange] = useState<Range | null>(null);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [detail, setDetail] = useState<ManagerRequestDetail | null>(null);
  const [decisionError, setDecisionError] = useState<string | null>(null);
  const [decisionLoading, setDecisionLoading] = useState(false);

  const isManager = useMemo(() => data?.roles?.some((r) => r.toUpperCase() === "MANAGER"), [data?.roles]);

  useEffect(() => {
    if (isManager && data?.managerPending?.length && !selectedId) {
      const first = data.managerPending[0].requestId;
      setSelectedId(first);
      fetchDetail(first);
    }
  }, [isManager, data?.managerPending, selectedId]);

  const refreshRange = async (nextRange: Range) => {
    setRange(nextRange);
    setLoading(true);
    setError(null);
    try {
      const payload = await fetchCalendarData(nextRange);
      setData(payload);
      if (isManager && payload.managerPending.length > 0) {
        const pendingMatch = payload.managerPending.find((p) => p.requestId === selectedId);
        const fallback = pendingMatch ? pendingMatch.requestId : payload.managerPending[0].requestId;
        setSelectedId(fallback);
        fetchDetail(fallback);
      } else {
        setSelectedId(null);
        setDetail(null);
      }
    } catch (err: any) {
      if (err?.message === "unauthorized") {
        window.location.assign("/login");
        return;
      }
      setError("Unable to refresh calendar for that range.");
    } finally {
      setLoading(false);
    }
  };

  const fetchDetail = async (requestId: string) => {
    setDecisionError(null);
    try {
      const response = await fetch(`${apiBase}/api/manager/request/${requestId}`, { credentials: "include" });
      if (response.status === 401) {
        window.location.assign("/login");
        return;
      }
      if (response.status === 404) {
        setDetail(null);
        return;
      }
      if (!response.ok) {
        setDecisionError("Unable to load request detail.");
        return;
      }
      const payload = (await response.json()) as ManagerRequestDetail;
      setDetail(payload);
    } catch (err) {
      setDecisionError("Unable to load request detail.");
    }
  };

  const handleDecision = async (approve: boolean, note: string) => {
    if (!selectedId) return;
    setDecisionLoading(true);
    setDecisionError(null);
    try {
      const url = `${apiBase}/api/manager/request/${selectedId}/${approve ? "approve" : "deny"}`;
      const response = await fetch(url, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify({ note })
      });
      if (response.status === 401) {
        window.location.assign("/login");
        return;
      }
      if (response.status === 503) {
        const body = await response.json().catch(() => ({}));
        setDecisionError(body.message ?? "External balance system unavailable.");
        return;
      }
      if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        setDecisionError(body.message ?? "Unable to process decision.");
        return;
      }
      await refreshRange(range);
      if (selectedId) {
        await fetchDetail(selectedId);
      }
    } catch (err) {
      setDecisionError("Unable to process decision.");
    } finally {
      setDecisionLoading(false);
    }
  };

  return (
    <>
      <Head>
        <title>Vacation Tool | Calendar</title>
      </Head>
      <div className="dashboard-shell">
        <header className="dashboard-header">
          <div>
            <p className="eyebrow">Calendar</p>
            <h1 className="page-title">Vacations</h1>
            <p className="muted">Shared calendar with role-aware actions.</p>
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
              onDraftCreate={(draft) => {
                setDraftRange(draft);
                setDialogOpen(true);
              }}
            />
          )}
          <MyVacationList vacations={data?.myVacations ?? []} />
        </div>

        {isManager && (
          <div className="dashboard-grid">
            <PendingRequestsList
              items={data?.managerPending ?? []}
              selectedId={selectedId}
              onSelect={(id) => {
                setSelectedId(id);
                fetchDetail(id);
              }}
            />
            <RequestDecisionPanel
              detail={detail}
              loading={decisionLoading}
              error={decisionError}
              onApprove={(note) => handleDecision(true, note)}
              onDeny={(note) => handleDecision(false, note)}
            />
          </div>
        )}

        <VacationRequestDialog
          open={dialogOpen}
          draftRange={draftRange}
          balance={data?.balance ?? null}
          onClose={() => setDialogOpen(false)}
          onSubmitted={async () => {
            await refreshRange(range);
          }}
        />
      </div>
    </>
  );
}
