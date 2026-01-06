import { fireEvent, render, screen } from "@testing-library/react";
import UserDrawer from "../components/UserDrawer";

describe("UserDrawer", () => {
  it("shows validation when required fields are missing", () => {
    render(
      <UserDrawer open onClose={() => {}} onSaved={() => {}} teams={[]} roles={["EMPLOYEE", "MANAGER"]} user={null} />
    );

    fireEvent.click(screen.getByText(/Create user/i));

    expect(screen.getByText(/Username and display name are required/i)).toBeInTheDocument();
  });
});
