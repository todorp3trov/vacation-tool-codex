import { fetchHrQueue, fetchHrRequestDetail } from "../../pages/hr/processing";

describe("hr-processing-success", () => {
  afterEach(() => {
    jest.resetAllMocks();
  });

  it("loads approved queue and request detail", async () => {
    const queuePayload = [
      {
        requestId: "req-1",
        employeeId: "emp-1",
        employeeName: "Employee One",
        startDate: "2024-02-01",
        endDate: "2024-02-05",
        numberOfDays: 3,
        status: "APPROVED",
        requestCode: "VR-req-1",
        managerNotes: "ok",
        hrNotes: null
      }
    ];
    const detailPayload = { request: queuePayload[0] };
    (global as any).fetch = jest
      .fn()
      .mockResolvedValueOnce({ ok: true, status: 200, json: () => Promise.resolve(queuePayload) })
      .mockResolvedValueOnce({ ok: true, status: 200, json: () => Promise.resolve(detailPayload) });

    const queue = await fetchHrQueue();
    const detail = await fetchHrRequestDetail(queue[0].requestId);

    expect(queue).toHaveLength(1);
    expect(detail.request.requestId).toEqual("req-1");
    expect((global as any).fetch).toHaveBeenCalledTimes(2);
  });
});
