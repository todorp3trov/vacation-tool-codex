export type VacationItem = {
  id: string;
  employeeName: string;
  startDate: string;
  endDate: string;
  status: string;
  mine: boolean;
  numberOfDays?: number;
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

export type CalendarPayload = {
  roles: string[];
  balance: BalanceSummary;
  myVacations: VacationItem[];
  teammateVacations: VacationItem[];
  holidays: HolidayItem[];
  managerPending: ManagerPendingItem[];
};

export type ComputeDaysResponse = {
  number_of_days: number;
};

export type SubmissionResponse = {
  requestId: string;
  requestCode: string;
  status: string;
  number_of_days: number;
};

export type ManagerPendingItem = {
  requestId: string;
  employeeId: string;
  employeeName: string;
  startDate: string;
  endDate: string;
  numberOfDays: number;
  status: string;
  balance?: BalanceSummary | null;
};

export type ManagerRequestDetail = {
  request: ManagerPendingItem;
  holidays: HolidayItem[];
  overlaps: VacationItem[];
};

export type ManagerCalendarResponse = {
  vacations: VacationItem[];
  holidays: HolidayItem[];
};
