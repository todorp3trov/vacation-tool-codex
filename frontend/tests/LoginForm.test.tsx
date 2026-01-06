import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import LoginForm from "../components/LoginForm";

describe("LoginForm", () => {
  beforeEach(() => {
    // @ts-expect-error allow override for tests
    global.fetch = jest.fn();
  });

  afterEach(() => {
    jest.resetAllMocks();
  });

  it("validates required fields before submit", async () => {
    render(<LoginForm />);

    fireEvent.click(screen.getByRole("button", { name: /sign in/i }));

    expect(await screen.findByText(/username and password are required/i)).toBeInTheDocument();
    expect(global.fetch).not.toHaveBeenCalled();
  });

  it("invokes success handler with home route", async () => {
    const onLoginSuccess = jest.fn();
    // @ts-expect-error allow override
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ homeRoute: "/dashboard" })
    });
    render(<LoginForm onLoginSuccess={onLoginSuccess} />);

    await userEvent.type(screen.getByLabelText(/username/i), "alice");
    await userEvent.type(screen.getByLabelText(/password/i), "pw");
    fireEvent.click(screen.getByRole("button", { name: /sign in/i }));

    await waitFor(() => expect(onLoginSuccess).toHaveBeenCalledWith("/dashboard"));
  });

  it("prevents duplicate submissions while loading", async () => {
    const slowFetch = new Promise((resolve) => setTimeout(() => resolve({
      ok: false,
      json: () => Promise.resolve({ error: "Invalid username or password" })
    }), 50));
    // @ts-expect-error allow override
    global.fetch = jest.fn().mockReturnValue(slowFetch);
    render(<LoginForm />);

    await userEvent.type(screen.getByLabelText(/username/i), "bob");
    await userEvent.type(screen.getByLabelText(/password/i), "secret");
    const button = screen.getByRole("button", { name: /sign in/i });
    fireEvent.click(button);
    fireEvent.click(button);

    await waitFor(() => expect(button).toBeDisabled());
    expect(global.fetch).toHaveBeenCalledTimes(1);
  });
});
