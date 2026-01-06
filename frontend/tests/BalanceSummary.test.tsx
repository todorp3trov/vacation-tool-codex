import { render, screen } from "@testing-library/react";
import BalanceSummary from "../components/BalanceSummary";

describe("BalanceSummary", () => {
  it("renders unavailable state", () => {
    render(<BalanceSummary officialBalance={null} tentativeBalance={null} unavailable message="External system down" />);

    expect(screen.getByText(/External system down/i)).toBeInTheDocument();
  });

  it("renders balances when available", () => {
    render(<BalanceSummary officialBalance={12} tentativeBalance={10} unavailable={false} />);

    expect(screen.getByText("12")).toBeInTheDocument();
    expect(screen.getByText("10")).toBeInTheDocument();
    expect(screen.queryByText(/Unavailable/i)).not.toBeInTheDocument();
  });
});
