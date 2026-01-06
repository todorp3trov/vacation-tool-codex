import { fetchDashboardData } from "../../pages/employee/dashboard";

describe("dashboard-balance-success", () => {
  afterEach(() => {
    jest.resetAllMocks();
  });

  it("loads dashboard data when backend responds", async () => {
    const payload = {
      balance: { officialBalance: 14, tentativeBalance: 12, unavailable: false },
      myVacations: [],
      teammateVacations: [],
      holidays: []
    };
    (global as any).fetch = jest.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: () => Promise.resolve(payload)
    });

    const data = await fetchDashboardData({ start: "2024-01-01", end: "2024-01-31" });

    expect(data.balance.unavailable).toBe(false);
    expect(global.fetch).toHaveBeenCalledTimes(1);
  });
});
