import { render, screen } from "@testing-library/react";
import HolidayImportForm from "../components/HolidayImportForm";

describe("HolidayImportForm", () => {
  it("disables import when integration is not ready", () => {
    render(<HolidayImportForm integrationReady={false} onImported={() => {}} />);

    expect(screen.getByText(/Run import/i)).toBeDisabled();
  });
});
