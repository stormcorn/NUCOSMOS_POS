export type NavigationOrderPreference = {
  rootOrder: string[];
  childOrders: Record<string, string[]>;
  updatedAt?: string | null;
};
