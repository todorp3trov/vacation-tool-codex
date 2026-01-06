export type VacationItem = {
  id: string;
  employeeName: string;
  startDate: string;
  endDate: string;
  status: string;
  mine: boolean;
};

export type HolidayItem = {
  id: string;
  date: string;
  name: string;
};

export type BalanceSummary = {
  officialBalance: number | null;
  tentativeBalance: number | null;
  unavailable: boolean;
  message?: string | null;
};

export type DashboardPayload = {
  balance: BalanceSummary;
  myVacations: VacationItem[];
  teammateVacations: VacationItem[];
  holidays: HolidayItem[];
};
