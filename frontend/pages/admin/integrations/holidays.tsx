import Head from "next/head";
import { GetServerSideProps } from "next";
import { useEffect, useState } from "react";
import HolidayImportForm from "../../../components/HolidayImportForm";
import ImportedHolidaysTable from "../../../components/ImportedHolidaysTable";
import { HolidayAdminItem, HolidayImportSummary, IntegrationConfig } from "../../../types/admin";

type Props = {
  initialConfigs: IntegrationConfig[];
  initialYears: number[];
  initialHolidays: HolidayAdminItem[];
  initialSelectedYear: number | null;
  error?: string | null;
};

const apiBase = (process.env.NEXT_PUBLIC_API_BASE ?? "http://localhost:8080").replace(/\/$/, "");

async function fetchConfigs(cookie?: string): Promise<IntegrationConfig[]> {
  const response = await fetch(`${apiBase}/api/admin/integrations`, {
    credentials: "include",
    headers: cookie ? { cookie } : undefined
  });
  if (response.status === 401) throw new Error("unauthorized");
  if (!response.ok) throw new Error("configs_load_failed");
  return (await response.json()) as IntegrationConfig[];
}

async function fetchYears(cookie?: string): Promise<number[]> {
  const response = await fetch(`${apiBase}/api/admin/holidays/years`, {
    credentials: "include",
    headers: cookie ? { cookie } : undefined
  });
  if (response.status === 401) throw new Error("unauthorized");
  if (!response.ok) throw new Error("years_load_failed");
  return (await response.json()) as number[];
}

async function fetchHolidays(year: number, cookie?: string): Promise<HolidayAdminItem[]> {
  const response = await fetch(`${apiBase}/api/admin/holidays/imported?year=${year}`, {
    credentials: "include",
    headers: cookie ? { cookie } : undefined
  });
  if (response.status === 401) throw new Error("unauthorized");
  if (!response.ok) throw new Error("holidays_load_failed");
  return (await response.json()) as HolidayAdminItem[];
}

export const getServerSideProps: GetServerSideProps<Props> = async (context) => {
  try {
    const [configs, years] = await Promise.all([
      fetchConfigs(context.req.headers.cookie ?? undefined),
      fetchYears(context.req.headers.cookie ?? undefined)
    ]);
    const selectedYear = years[0] ?? null;
    const holidays = selectedYear ? await fetchHolidays(selectedYear, context.req.headers.cookie ?? undefined) : [];
    return {
      props: {
        initialConfigs: configs,
        initialYears: years,
        initialHolidays: holidays,
        initialSelectedYear: selectedYear,
        error: null
      }
    };
  } catch (error: any) {
    if (error?.message === "unauthorized") {
      return { redirect: { destination: "/login", permanent: false } };
    }
    return { props: { initialConfigs: [], initialYears: [], initialHolidays: [], initialSelectedYear: null, error: "Unable to load integration controls." } };
  }
};

export default function HolidayIntegrationPage({
  initialConfigs,
  initialYears,
  initialHolidays,
  initialSelectedYear,
  error: initialError
}: Props) {
  const [configs, setConfigs] = useState<IntegrationConfig[]>(initialConfigs);
  const [years, setYears] = useState<number[]>(initialYears);
  const [selectedYear, setSelectedYear] = useState<number | null>(initialSelectedYear);
  const [holidays, setHolidays] = useState<HolidayAdminItem[]>(initialHolidays);
  const [error, setError] = useState<string | null>(initialError ?? null);
  const [configEndpoint, setConfigEndpoint] = useState("");
  const [configToken, setConfigToken] = useState("");
  const [configMessage, setConfigMessage] = useState<string | null>(null);
  const [configError, setConfigError] = useState<string | null>(null);
  const [savingConfig, setSavingConfig] = useState(false);

  const holidayConfig = configs.find((c) => c.type.toUpperCase() === "HOLIDAY_API") ?? null;

  useEffect(() => {
    if (holidayConfig) {
      setConfigEndpoint(holidayConfig.endpointUrl ?? "");
      setConfigToken("");
    }
  }, [holidayConfig?.id]);

  const handleSaveConfig = async () => {
    if (!configEndpoint.trim()) {
      setConfigError("Endpoint is required");
      return;
    }
    setSavingConfig(true);
    setConfigError(null);
    setConfigMessage(null);
    const payload = { type: "HOLIDAY_API", endpointUrl: configEndpoint, authToken: configToken || undefined };
    const url = holidayConfig ? `${apiBase}/api/admin/integrations/${holidayConfig.id}` : `${apiBase}/api/admin/integrations`;
    const method = holidayConfig ? "PUT" : "POST";
    try {
      const response = await fetch(url, {
        method,
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
      });
      if (response.status === 401) {
        window.location.assign("/login");
        return;
      }
      const body = await response.json().catch(() => ({}));
      if (!response.ok) {
        setConfigError(body.error ?? "Unable to save integration config.");
        return;
      }
      setConfigs((prev) => {
        const others = prev.filter((c) => c.id !== body.id);
        return [...others, body as IntegrationConfig];
      });
      setConfigMessage("Integration config saved.");
      setConfigToken("");
    } catch {
      setConfigError("Unable to save integration config.");
    } finally {
      setSavingConfig(false);
    }
  };

  const handleDisable = async () => {
    if (!holidayConfig) return;
    setSavingConfig(true);
    setConfigError(null);
    setConfigMessage(null);
    try {
      const response = await fetch(`${apiBase}/api/admin/integrations/${holidayConfig.id}/disable`, {
        method: "POST",
        credentials: "include"
      });
      if (response.status === 401) {
        window.location.assign("/login");
        return;
      }
      if (!response.ok) {
        setConfigError("Unable to disable integration.");
        return;
      }
      const body = await response.json();
      setConfigs((prev) => prev.map((c) => (c.id === body.id ? (body as IntegrationConfig) : c)));
      setConfigMessage("Integration disabled.");
    } catch {
      setConfigError("Unable to disable integration.");
    } finally {
      setSavingConfig(false);
    }
  };

  const loadHolidays = async (year: number) => {
    try {
      const items = await fetchHolidays(year);
      setHolidays(items);
      setSelectedYear(year);
    } catch (err: any) {
      if (err?.message === "unauthorized") {
        window.location.assign("/login");
        return;
      }
      setError("Unable to load holidays for that year.");
    }
  };

  const handleImported = async (summary: HolidayImportSummary) => {
    const { year } = summary;
    if (!years.includes(year)) {
      setYears((prev) => [year, ...prev]);
    }
    await loadHolidays(year);
  };

  const handleDeprecate = async (holidayId: string) => {
    const reason = window.prompt("Provide a deprecation note (optional):") ?? "";
    try {
      const response = await fetch(`${apiBase}/api/admin/holidays/${holidayId}/deprecate`, {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ reason })
      });
      if (response.status === 401) {
        window.location.assign("/login");
        return;
      }
      if (!response.ok) {
        setError("Unable to deprecate holiday.");
        return;
      }
      if (selectedYear) {
        await loadHolidays(selectedYear);
        try {
          const refreshedYears = await fetchYears();
          setYears(refreshedYears);
          if (refreshedYears.length && !refreshedYears.includes(selectedYear)) {
            setSelectedYear(refreshedYears[0]);
            const items = await fetchHolidays(refreshedYears[0]);
            setHolidays(items);
          }
        } catch (err: any) {
          if (err?.message === "unauthorized") {
            window.location.assign("/login");
            return;
          }
        }
      }
    } catch {
      setError("Unable to deprecate holiday.");
    }
  };

  const maskedEndpoint = holidayConfig?.endpointUrl ? holidayConfig.endpointUrl.replace(/https?:\/\//, "") : "Not set";
  const integrationReady = holidayConfig?.state === "CONFIGURED";

  return (
    <>
      <Head>
        <title>Vacation Tool | Holiday Integration</title>
      </Head>
      <div className="dashboard-shell">
        <header className="dashboard-header">
          <div>
            <p className="eyebrow">Admin</p>
            <h1 className="page-title">Holiday integration</h1>
            <p className="muted">Configure the holiday API and run imports by year.</p>
          </div>
          <div className="header-actions">
            <button className="ghost-btn" onClick={() => window.location.assign("/admin/users")}>
              Back to admin
            </button>
          </div>
        </header>

        {(error || initialError) && <div className="alert alert-error">{error ?? initialError}</div>}

        <div className="dashboard-grid">
          <div className="summary-card">
            <div className="card-header">
              <div>
                <p className="eyebrow">Integration config</p>
                <h3 className="card-title">Holiday API endpoint</h3>
              </div>
              <span className={`pill ${integrationReady ? "approved" : "pending"}`}>{integrationReady ? "Configured" : "Disabled"}</span>
            </div>
            <div className="form-field">
              <span className="label">Endpoint URL</span>
              <input type="text" value={configEndpoint} onChange={(e) => setConfigEndpoint(e.target.value)} disabled={savingConfig} />
              <p className="muted small">Current: {maskedEndpoint}</p>
            </div>
            <div className="form-field">
              <span className="label">Auth token (optional)</span>
              <input
                type="password"
                value={configToken}
                onChange={(e) => setConfigToken(e.target.value)}
                placeholder={holidayConfig?.hasAuthToken ? "Token stored — set to rotate" : "Add token"}
                disabled={savingConfig}
              />
            </div>
            <div className="dialog-actions" style={{ justifyContent: "flex-start" }}>
              <button className="primary-btn" onClick={handleSaveConfig} disabled={savingConfig}>
                {savingConfig ? "Saving…" : "Save config"}
              </button>
              {holidayConfig && (
                <button className="ghost-btn" onClick={handleDisable} disabled={savingConfig}>
                  Disable
                </button>
              )}
            </div>
            {configError && <div className="alert alert-error">{configError}</div>}
            {configMessage && <div className="alert alert-warning">{configMessage}</div>}
          </div>

          <HolidayImportForm onImported={handleImported} integrationReady={integrationReady ?? false} />
        </div>

        <ImportedHolidaysTable
          years={years}
          selectedYear={selectedYear}
          holidays={holidays}
          onSelectYear={(year) => loadHolidays(year)}
          onDeprecate={handleDeprecate}
        />
      </div>
    </>
  );
}
