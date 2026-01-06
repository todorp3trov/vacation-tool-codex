import { act } from "react-dom/test-utils";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import LoginForm from "../components/LoginForm";

describe("LoginForm", () => {
  beforeEach(() => {
    (global as any).fetch = jest.fn();
  });

  afterEach(() => {
    jest.resetAllMocks();
  });

  it("validates required fields before submit", async () => {
    render(<LoginForm />);

    await act(async () => {
      await userEvent.click(screen.getByRole("button", { name: /sign in/i }));
    });

    expect(await screen.findByText(/username and password are required/i)).toBeInTheDocument();
    expect(global.fetch).not.toHaveBeenCalled();
  });

  it("invokes success handler with home route", async () => {
    const onLoginSuccess = jest.fn();
    (global as any).fetch = jest.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ homeRoute: "/dashboard" })
    });
    render(<LoginForm onLoginSuccess={onLoginSuccess} />);

    await act(async () => {
      await userEvent.type(screen.getByLabelText(/username/i), "alice");
      await userEvent.type(screen.getByLabelText(/password/i), "pw");
      await userEvent.click(screen.getByRole("button", { name: /sign in/i }));
    });

    await waitFor(() => expect(onLoginSuccess).toHaveBeenCalledWith("/dashboard"));
  });

  it("prevents duplicate submissions while loading", async () => {
    let resolveFetch: (value: any) => void = () => {};
    const slowFetch = new Promise((resolve) => { resolveFetch = resolve; });
    (global as any).fetch = jest.fn().mockReturnValue(slowFetch);
    render(<LoginForm />);

    await act(async () => {
      await userEvent.type(screen.getByLabelText(/username/i), "bob");
      await userEvent.type(screen.getByLabelText(/password/i), "secret");
    });
    const button = screen.getByRole("button", { name: /sign in/i });
    await act(async () => {
      await userEvent.click(button);
    });
    await waitFor(() => expect(button).toBeDisabled());
    await act(async () => {
      await userEvent.click(button);
    });

    await act(async () => {
      resolveFetch({
        ok: false,
        json: () => Promise.resolve({ error: "Invalid username or password" })
      });
    });
    expect(global.fetch).toHaveBeenCalledTimes(1);
  });
});
