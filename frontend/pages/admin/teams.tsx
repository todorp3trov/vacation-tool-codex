import Head from "next/head";
import { GetServerSideProps } from "next";
import { useState } from "react";
import TeamDialog from "../../components/TeamDialog";
import { AdminTeam, AdminUser } from "../../types/admin";

type Props = {
  initialTeams: AdminTeam[];
  initialUsers: AdminUser[];
  error?: string | null;
};

const apiBase = (process.env.NEXT_PUBLIC_API_BASE ?? "http://localhost:8080").replace(/\/$/, "");

async function fetchTeams(cookie?: string): Promise<AdminTeam[]> {
  const response = await fetch(`${apiBase}/api/admin/teams`, {
    credentials: "include",
    headers: cookie ? { cookie } : undefined
  });
  if (response.status === 401) throw new Error("unauthorized");
  if (!response.ok) throw new Error("teams_load_failed");
  return (await response.json()) as AdminTeam[];
}

async function fetchUsers(cookie?: string): Promise<AdminUser[]> {
  const response = await fetch(`${apiBase}/api/admin/users`, {
    credentials: "include",
    headers: cookie ? { cookie } : undefined
  });
  if (response.status === 401) throw new Error("unauthorized");
  if (!response.ok) throw new Error("users_load_failed");
  return (await response.json()) as AdminUser[];
}

export const getServerSideProps: GetServerSideProps<Props> = async (context) => {
  try {
    const [teams, users] = await Promise.all([
      fetchTeams(context.req.headers.cookie ?? undefined),
      fetchUsers(context.req.headers.cookie ?? undefined)
    ]);
    return { props: { initialTeams: teams, initialUsers: users, error: null } };
  } catch (error: any) {
    if (error?.message === "unauthorized") {
      return {
        redirect: { destination: "/login", permanent: false }
      };
    }
    return { props: { initialTeams: [], initialUsers: [], error: "Unable to load teams." } };
  }
};

export default function TeamsPage({ initialTeams, initialUsers, error: initialError }: Props) {
  const [teams, setTeams] = useState<AdminTeam[]>(initialTeams);
  const [users, setUsers] = useState<AdminUser[]>(initialUsers);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [selectedTeam, setSelectedTeam] = useState<AdminTeam | null>(null);
  const [error, setError] = useState<string | null>(initialError ?? null);

  const openCreate = () => {
    setSelectedTeam(null);
    setDialogOpen(true);
  };

  const openEdit = (team: AdminTeam) => {
    setSelectedTeam(team);
    setDialogOpen(true);
  };

  const handleSaved = async (saved: AdminTeam) => {
    setTeams((prev) => {
      const existing = prev.find((t) => t.id === saved.id);
      if (existing) {
        return prev.map((t) => (t.id === saved.id ? saved : t));
      }
      return [saved, ...prev];
    });
    try {
      const freshUsers = await fetchUsers();
      setUsers(freshUsers);
    } catch {
      // ignore
    }
  };

  const memberNames = (memberIds: string[]) =>
    memberIds
      .map((id) => users.find((u) => u.id === id)?.displayName ?? "Unknown")
      .slice(0, 3)
      .join(", ");

  return (
    <>
      <Head>
        <title>Vacation Tool | Teams</title>
      </Head>
      <div className="dashboard-shell">
        <header className="dashboard-header">
          <div>
            <p className="eyebrow">Admin</p>
            <h1 className="page-title">Teams</h1>
            <p className="muted">Define visibility groups and membership.</p>
          </div>
          <div className="header-actions">
            <button className="ghost-btn" onClick={() => window.location.assign("/admin/users")}>
              Back to users
            </button>
            <button className="primary-btn" onClick={openCreate}>
              New team
            </button>
          </div>
        </header>

        {(error || initialError) && <div className="alert alert-error">{error ?? initialError}</div>}

        <div className="summary-card">
          <div className="card-header">
            <h3 className="card-title">Teams</h3>
            <span className="pill neutral">{teams.length} total</span>
          </div>
          {teams.length === 0 ? (
            <p className="muted">No teams set up yet.</p>
          ) : (
            <div className="table">
              <div className="table-head">
                <span>Name</span>
                <span>Status</span>
                <span>Members</span>
                <span></span>
              </div>
              {teams.map((team) => (
                <div key={team.id} className="table-row">
                  <div>
                    <strong>{team.name}</strong>
                    <p className="muted small">{team.memberIds.length} members</p>
                  </div>
                  <span className={`pill ${team.status === "ACTIVE" ? "approved" : "pending"}`}>{team.status}</span>
                  <div>
                    {team.memberIds.length === 0 ? (
                      <span className="muted small">No members</span>
                    ) : (
                      <span className="muted small">{memberNames(team.memberIds)}</span>
                    )}
                  </div>
                  <button className="ghost-btn" onClick={() => openEdit(team)}>
                    Edit
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      <TeamDialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        onSaved={handleSaved}
        users={users}
        team={selectedTeam}
      />
    </>
  );
}
