import Head from "next/head";
import { GetServerSideProps } from "next";
import { useEffect, useState } from "react";
import ApprovedRequestsQueue from "../../components/ApprovedRequestsQueue";
import RequestProcessingPanel from "../../components/RequestProcessingPanel";
import { HrProcessingDetail, HrProcessingItem } from "../../types/dashboard";

type Props = {
  initialQueue: HrProcessingItem[];
  initialDetail: HrProcessingDetail | null;
  error?: string | null;
};

const apiBase = (process.env.NEXT_PUBLIC_API_BASE ?? "http://localhost:8080").replace(/\/$/, "");

export async function fetchHrQueue(cookie?: string): Promise<HrProcessingItem[]> {
  const response = await fetch(`${apiBase}/api/hr/queue`, {
    credentials: "include",
    headers: cookie ? { cookie } : undefined
  });
  if (response.status === 401) {
    throw new Error("unauthorized");
  }
  if (!response.ok) {
    throw new Error("hr_queue_load_failed");
  }
  return (await response.json()) as HrProcessingItem[];
}

export async function fetchHrRequestDetail(requestId: string, cookie?: string): Promise<HrProcessingDetail> {
  const response = await fetch(`${apiBase}/api/hr/request/${requestId}`, {
    credentials: "include",
    headers: cookie ? { cookie } : undefined
  });
  if (response.status === 401) {
    throw new Error("unauthorized");
  }
  if (response.status === 404) {
    throw new Error("not_found");
  }
  if (!response.ok) {
    throw new Error("hr_detail_load_failed");
  }
  return (await response.json()) as HrProcessingDetail;
}

export async function processHrRequest(requestId: string, hrNotes?: string) {
  const response = await fetch(`${apiBase}/api/hr/process`, {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ requestId, hrNotes })
  });
  if (response.status === 401) {
    throw new Error("unauthorized");
  }
  return response;
}

export const getServerSideProps: GetServerSideProps<Props> = async (context) => {
  try {
    const queue = await fetchHrQueue(context.req.headers.cookie ?? undefined);
    const initialDetail =
      queue.length > 0 ? await fetchHrRequestDetail(queue[0].requestId, context.req.headers.cookie ?? undefined) : null;
    return { props: { initialQueue: queue, initialDetail, error: null } };
  } catch (error: any) {
    if (error?.message === "unauthorized") {
      return {
        redirect: {
          destination: "/login",
          permanent: false
        }
      };
    }
    return { props: { initialQueue: [], initialDetail: null, error: "Unable to load HR processing queue." } };
  }
};

export default function HrProcessingPage({ initialQueue, initialDetail, error: initialError }: Props) {
  const [queue, setQueue] = useState<HrProcessingItem[]>(initialQueue);
  const [selectedId, setSelectedId] = useState<string | null>(initialDetail?.request.requestId ?? initialQueue[0]?.requestId ?? null);
  const [detail, setDetail] = useState<HrProcessingDetail | null>(initialDetail);
  const [pageError, setPageError] = useState<string | null>(initialError ?? null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);
  const [processing, setProcessing] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);

  useEffect(() => {
    setActionError(null);
    setActionSuccess(null);
  }, [selectedId]);

  useEffect(() => {
    if (selectedId && (!detail || detail.request.requestId !== selectedId)) {
      loadDetail(selectedId);
    }
  }, [selectedId]);

  const loadDetail = async (requestId: string) => {
    setDetailLoading(true);
    setActionError(null);
    try {
      const payload = await fetchHrRequestDetail(requestId);
      setDetail(payload);
    } catch (err: any) {
      if (err?.message === "unauthorized") {
        window.location.assign("/login");
        return;
      }
      if (err?.message === "not_found") {
        setDetail(null);
        setPageError("Request not found or already processed.");
        return;
      }
      setPageError("Unable to load request detail.");
    } finally {
      setDetailLoading(false);
    }
  };

  const handleProcess = async (notes: string) => {
    if (!selectedId) return;
    if (!window.confirm("Mark this request as processed and deduct days?")) {
      return;
    }
    setProcessing(true);
    setActionError(null);
    setActionSuccess(null);
    try {
      const response = await processHrRequest(selectedId, notes);
      if (response.status === 503) {
        const body = await response.json().catch(() => ({}));
        setActionError(body.message ?? "External balance system unavailable.");
        return;
      }
      if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        setActionError(body.message ?? "Unable to process request.");
        return;
      }
      const payload = (await response.json()) as HrProcessingDetail;
      const updatedQueue = queue.filter((item) => item.requestId !== selectedId);
      setQueue(updatedQueue);
      setActionSuccess("Request processed and deducted.");
      if (updatedQueue.length === 0) {
        setSelectedId(null);
        setDetail(payload);
      } else {
        const nextId = updatedQueue[0].requestId;
        setSelectedId(nextId);
        await loadDetail(nextId);
      }
    } catch (err: any) {
      if (err?.message === "unauthorized") {
        window.location.assign("/login");
        return;
      }
      setActionError("Unable to process request.");
    } finally {
      setProcessing(false);
    }
  };

  return (
    <>
      <Head>
        <title>Vacation Tool | HR Processing</title>
      </Head>
      <div className="dashboard-shell">
        <header className="dashboard-header">
          <div>
            <p className="eyebrow">HR</p>
            <h1 className="page-title">Processing queue</h1>
            <p className="muted">Approved requests ready for deduction.</p>
          </div>
          <div className="header-actions">
            <button className="ghost-btn" onClick={() => window.location.assign("/calendar")}>
              Back to Calendar
            </button>
          </div>
        </header>

        {pageError && <div className="alert alert-error">{pageError}</div>}

        <div className="dashboard-grid">
          <ApprovedRequestsQueue items={queue} selectedId={selectedId} onSelect={setSelectedId} />
          <RequestProcessingPanel
            detail={detail}
            loading={processing || detailLoading}
            error={actionError}
            successMessage={actionSuccess}
            onProcess={handleProcess}
          />
        </div>
      </div>
    </>
  );
}
