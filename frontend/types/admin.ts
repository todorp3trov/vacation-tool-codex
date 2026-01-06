export type AdminUser = {
  id: string;
  username: string;
  displayName: string;
  status: string;
  roles: string[];
  teamIds: string[];
  createdAt?: string;
  updatedAt?: string;
};

export type AdminTeam = {
  id: string;
  name: string;
  status: string;
  memberIds: string[];
  createdAt?: string;
};

export type IntegrationConfig = {
  id: string;
  type: string;
  state: string;
  endpointUrl: string;
  hasAuthToken: boolean;
  updatedAt?: string;
};

export type HolidayAdminItem = {
  id: string;
  date: string;
  name: string;
  status: string;
  deprecationReason?: string | null;
};

export type HolidayImportSummary = {
  year: number;
  imported: number;
  skipped: number;
  outcome: string;
  message: string;
};
