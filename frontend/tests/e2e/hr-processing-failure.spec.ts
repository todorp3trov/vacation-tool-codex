import { processHrRequest } from "../../pages/hr/processing";

describe("hr-processing-failure", () => {
  afterEach(() => {
    jest.resetAllMocks();
  });

  it("surfaces external unavailability errors", async () => {
    (global as any).fetch = jest.fn().mockResolvedValue({
      ok: false,
      status: 503,
      json: () => Promise.resolve({ message: "External balance system unavailable." })
    });

    const response = await processHrRequest("req-1", "note");

    expect(response.status).toBe(503);
    expect((global as any).fetch).toHaveBeenCalledWith(
      expect.stringContaining("/api/hr/process"),
      expect.objectContaining({ method: "POST" })
    );
  });
});
