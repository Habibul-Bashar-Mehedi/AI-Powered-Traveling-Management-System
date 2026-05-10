export enum UserRole {
  USER = 'USER',
  ADMIN = 'ADMIN',
  VENDOR = 'VENDOR'
}

export const UserRoleLabels: Record<UserRole, string> = {
  [UserRole.USER]: 'User',
  [UserRole.ADMIN]: 'Admin',
  [UserRole.VENDOR]: 'Vendor'
};
