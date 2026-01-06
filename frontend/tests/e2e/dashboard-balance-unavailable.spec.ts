import { fetchDashboardData } from "../../pages/employee/dashboard";

describe("dashboard-balance-unavailable", () => {
  afterEach(() => {
    jest.resetAllMocks();
  });

  it("surfaces unavailable balance flag from API", async () => {
    const payload = {
      balance: { officialBalance: null, tentativeBalance: null, unavailable: true, message: "External system unavailable" },
      myVacations: [],
      teammateVacations: [],
      holidays: []
    };
    (global as any).fetch = jest.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: () => Promise.resolve(payload)
    });

    const data = await fetchDashboardData({ start: "2024-02-01", end: "2024-02-29" });

    expect(data.balance.unavailable).toBe(true);
    expect(data.balance.message).toMatch(/unavailable/i);
  });

  it("throws on unauthorized", async () => {
    (global as any).fetch = jest.fn().mockResolvedValue({
      ok: false,
      status: 401,
      json: () => Promise.resolve({})
    });

    await expect(fetchDashboardData({ start: "2024-02-01", end: "2024-02-29" })).rejects.toThrow("unauthorized");
  });
});
