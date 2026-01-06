import Head from "next/head";
import { GetServerSideProps } from "next";
import { useState } from "react";
import UserDrawer from "../../components/UserDrawer";
import { AdminTeam, AdminUser } from "../../types/admin";

type Props = {
  initialUsers: AdminUser[];
  initialTeams: AdminTeam[];
  availableRoles: string[];
  error?: string | null;
};

const apiBase = (process.env.NEXT_PUBLIC_API_BASE ?? "http://localhost:8080").replace(/\/$/, "");

export async function fetchUsers(cookie?: string): Promise<AdminUser[]> {
  const response = await fetch(`${apiBase}/api/admin/users`, {
    credentials: "include",
    headers: cookie ? { cookie } : undefined
  });
  if (response.status === 401) throw new Error("unauthorized");
  if (!response.ok) throw new Error("users_load_failed");
  return (await response.json()) as AdminUser[];
}

export async function fetchTeams(cookie?: string): Promise<AdminTeam[]> {
  const response = await fetch(`${apiBase}/api/admin/teams`, {
    credentials: "include",
    headers: cookie ? { cookie } : undefined
  });
  if (response.status === 401) throw new Error("unauthorized");
  if (!response.ok) throw new Error("teams_load_failed");
  return (await response.json()) as AdminTeam[];
}

export async function fetchRoles(cookie?: string): Promise<string[]> {
  const response = await fetch(`${apiBase}/api/admin/roles`, {
    credentials: "include",
    headers: cookie ? { cookie } : undefined
  });
  if (response.status === 401) throw new Error("unauthorized");
  if (!response.ok) throw new Error("roles_load_failed");
  return (await response.json()) as string[];
}

export const getServerSideProps: GetServerSideProps<Props> = async (context) => {
  try {
    const [users, teams, roles] = await Promise.all([
      fetchUsers(context.req.headers.cookie ?? undefined),
      fetchTeams(context.req.headers.cookie ?? undefined),
      fetchRoles(context.req.headers.cookie ?? undefined)
    ]);
    return { props: { initialUsers: users, initialTeams: teams, availableRoles: roles, error: null } };
  } catch (error: any) {
    if (error?.message === "unauthorized") {
      return {
        redirect: { destination: "/login", permanent: false }
      };
    }
    return { props: { initialUsers: [], initialTeams: [], availableRoles: [], error: "Unable to load users." } };
  }
};

export default function AdminUsersPage({ initialUsers, initialTeams, availableRoles, error: initialError }: Props) {
  const [users, setUsers] = useState<AdminUser[]>(initialUsers);
  const [teams, setTeams] = useState<AdminTeam[]>(initialTeams);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [selectedUser, setSelectedUser] = useState<AdminUser | null>(null);
  const [error, setError] = useState<string | null>(initialError ?? null);

  const openCreate = () => {
    setSelectedUser(null);
    setDrawerOpen(true);
  };

  const openEdit = (user: AdminUser) => {
    setSelectedUser(user);
    setDrawerOpen(true);
  };

  const handleSaved = async (saved: AdminUser) => {
    setUsers((prev) => {
      const existing = prev.find((u) => u.id === saved.id);
      if (existing) {
        return prev.map((u) => (u.id === saved.id ? saved : u));
      }
      return [saved, ...prev];
    });
    try {
      const freshTeams = await fetchTeams();
      setTeams(freshTeams);
    } catch {
      // ignore refresh error for now
    }
  };

  const teamName = (teamId: string) => teams.find((t) => t.id === teamId)?.name ?? "Unknown";

  return (
    <>
      <Head>
        <title>Vacation Tool | Admin Users</title>
      </Head>
      <div className="dashboard-shell">
        <header className="dashboard-header">
          <div>
            <p className="eyebrow">Admin</p>
            <h1 className="page-title">Users</h1>
            <p className="muted">Manage accounts, roles, and team assignments.</p>
          </div>
          <div className="header-actions">
            <button className="ghost-btn" onClick={() => window.location.assign("/admin/teams")}>
              Manage teams
            </button>
            <button className="primary-btn" onClick={openCreate}>
              New user
            </button>
          </div>
        </header>

        {(error || initialError) && <div className="alert alert-error">{error ?? initialError}</div>}

        <div className="summary-card">
          <div className="card-header">
            <h3 className="card-title">Directory</h3>
            <span className="pill neutral">{users.length} users</span>
          </div>
          {users.length === 0 ? (
            <p className="muted">No users yet. Create the first user to get started.</p>
          ) : (
            <div className="table">
              <div className="table-head">
                <span>User</span>
                <span>Roles</span>
                <span>Teams</span>
                <span>Status</span>
                <span></span>
              </div>
              {users.map((u) => (
                <div key={u.id} className="table-row">
                  <div>
                    <strong>{u.displayName}</strong>
                    <p className="muted small">{u.username}</p>
                  </div>
                  <div>
                    {u.roles.map((role) => (
                      <span key={role} className="pill team" style={{ marginRight: 6 }}>
                        {role}
                      </span>
                    ))}
                  </div>
                  <div>
                    {u.teamIds.length === 0 ? (
                      <span className="muted small">No team</span>
                    ) : (
                      u.teamIds.map((tid) => (
                        <span key={tid} className="pill neutral" style={{ marginRight: 6 }}>
                          {teamName(tid)}
                        </span>
                      ))
                    )}
                  </div>
                  <span className={`pill ${u.status === "ACTIVE" ? "approved" : "denied"}`}>{u.status}</span>
                  <button className="ghost-btn" onClick={() => openEdit(u)}>
                    Edit
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      <UserDrawer
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        onSaved={handleSaved}
        teams={teams}
        roles={availableRoles}
        user={selectedUser}
      />
    </>
  );
}
