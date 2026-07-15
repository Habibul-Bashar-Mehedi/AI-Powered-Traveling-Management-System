import { Component, OnInit, ChangeDetectorRef, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { AdminManagementService } from '../../services/admin-management.service';
import { AdminUser, AdminUserRequest } from '../../models/admin-management.model';
import { UserRole, UserRoleLabels } from '../../enums/user-role.enum';
import { VendorType } from '../../enums/vendor.enums';
import { FooterComponent } from '../../shared/app-footer/app-footer';
import { ConfirmDialogService } from '../../shared/confirm-dialog/confirm-dialog.service';

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, FooterComponent],
  templateUrl: './user-management.html',
  styleUrls: ['./user-management.css']
})
export class UserManagement implements OnInit {
  users: AdminUser[] = [];
  loading = false;
  saving = false;
  deletingId: string | null = null;
  error = '';
  success = '';

  search = '';
  roleFilter: UserRole | '' = '';
  page = 0;
  size = 10;
  totalPages = 0;
  totalElements = 0;

  modalOpen = false;
  editingUserId: string | null = null;
  form: AdminUserRequest = this.getEmptyForm();

  readonly roleOptions = [UserRole.USER, UserRole.ADMIN, UserRole.SUPER_ADMIN, UserRole.VENDOR];
  readonly roleLabels = UserRoleLabels;
  readonly vendorTypeOptions = Object.values(VendorType);

  constructor(
    private adminManagementService: AdminManagementService,
    private authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private confirmDialog: ConfirmDialogService,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    // Skip authenticated API calls during SSR — tokens aren't available server-side,
    // and Angular's non-destructive hydration never re-runs ngOnInit on the client,
    // so an SSR-time failure here would leave the page stuck until the component
    // is destroyed and recreated by a later client-side navigation.
    if (!isPlatformBrowser(this.platformId)) return;
    this.loadUsers();
  }

  loadUsers(resetPage = false): void {
    if (resetPage) {
      this.page = 0;
    }

    this.loading = true;
    this.error = '';

    this.adminManagementService.getUsers(this.page, this.size, this.search, this.roleFilter || undefined).subscribe({
      next: (res) => {
        this.users = res.content;
        this.totalPages = res.totalPages;
        this.totalElements = res.totalElements;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to load users';
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  nextPage(): void {
    if (this.page + 1 >= this.totalPages) {
      return;
    }

    this.page += 1;
    this.loadUsers();
  }

  prevPage(): void {
    if (this.page === 0) {
      return;
    }

    this.page -= 1;
    this.loadUsers();
  }

  onRoleFilterChange(): void {
    // Reset to page 0 and load users when role filter changes
    this.page = 0;
    this.loadUsers();
  }

  openCreateModal(): void {
    this.editingUserId = null;
    this.form = this.getEmptyForm();
    this.modalOpen = true;
    this.error = '';
    this.success = '';
  }

  openEditModal(user: AdminUser): void {
    this.editingUserId = user.id;
    this.form = {
      username: user.username,
      email: user.email,
      password: '',
      role: user.role,
      countryId: user.countryId || '',
      vendorType: user.vendorType || ''
    };
    this.modalOpen = true;
    this.error = '';
    this.success = '';
  }

  closeModal(): void {
    this.modalOpen = false;
    this.editingUserId = null;
    this.form = this.getEmptyForm();
  }

  saveUser(): void {
    if (!this.isValidForm()) {
      this.error = this.editingUserId
        ? 'Please fill all required fields. If you provide a new password, it must be at least 8 characters.'
        : 'Please fill all required fields and use a password with at least 8 characters.';
      return;
    }

    this.saving = true;
    this.error = '';
    this.success = '';

    const payload: AdminUserRequest = {
      username: this.form.username.trim(),
      email: this.form.email.trim().toLowerCase(),
      role: this.form.role,
      countryId: this.form.countryId?.trim() || undefined,
      vendorType: this.form.role === UserRole.VENDOR ? this.form.vendorType?.trim() || undefined : undefined
    };

    const trimmedPassword = this.form.password?.trim();
    if (!this.editingUserId || trimmedPassword) {
      payload.password = trimmedPassword;
    }

    const isCreate = !this.editingUserId;

    const req$ = this.editingUserId
      ? this.adminManagementService.updateUser(this.editingUserId, payload)
      : this.adminManagementService.createUser(payload);

    req$.subscribe({
      next: () => {
        this.saving = false;
        this.success = this.editingUserId ? 'User updated successfully.' : 'User created successfully.';
        this.closeModal();
        this.loadUsers(isCreate);
      },
      error: (err) => {
        this.error = this.resolveSaveErrorMessage(err);
        this.saving = false;
      }
    });
  }

  async deleteUser(user: AdminUser): Promise<void> {
    const ok = await this.confirmDialog.confirm({
      title: 'Delete this user?',
      message: `Delete ${user.email}? This cannot be undone.`,
      confirmLabel: 'Delete User',
      danger: true
    });
    if (!ok) {
      return;
    }

    this.deletingId = user.id;
    this.error = '';
    this.success = '';

    this.adminManagementService.deleteUser(user.id).subscribe({
      next: () => {
        this.deletingId = null;
        this.success = 'User deleted successfully.';
        this.loadUsers();
        this.cdr.markForCheck();
      },
      error: (err) => {
        // Handle CORS/Network errors (status 0)
        if (err?.status === 0) {
          this.error = 'Network error: Unable to connect to the server. Please check if the backend is running and CORS is properly configured.';
          this.deletingId = null;
          this.cdr.markForCheck();
          console.error('Delete user error (CORS/Network):', err);
          return;
        }

        // Handle constraint violation errors (409 Conflict or 500 with constraint info)
        if (err?.status === 500 || err?.status === 409) {
          this.error = 'Cannot delete user: User has associated data (bookings, orders, tokens, etc.). Please remove or reassign related data first.';
          this.deletingId = null;
          this.cdr.markForCheck();
          console.error('Delete user error (constraint):', err);
          return;
        }

        // Handle other errors
        const errorMessage = err?.error?.message || err?.message || 'Failed to delete user';
        this.error = errorMessage;
        this.deletingId = null;
        this.cdr.markForCheck();
        console.error('Delete user error:', err);
      }
    });
  }

  logout(): void {
    this.authService.logout().subscribe({ complete: () => this.router.navigate(['/login']) });
  }

  private isValidForm(): boolean {
    const hasBasicFields = !!(
      this.form.username?.trim() &&
      this.form.email?.trim() &&
      this.form.role
    );

    if (!hasBasicFields) {
      return false;
    }

    const trimmedPassword = this.form.password?.trim() || '';

    if (!this.editingUserId) {
      return trimmedPassword.length >= 8;
    }

    return trimmedPassword.length === 0 || trimmedPassword.length >= 8;
  }

  private getEmptyForm(): AdminUserRequest {
    return {
      username: '',
      email: '',
      password: '',
      role: UserRole.USER,
      countryId: '',
      vendorType: ''
    };
  }

  private resolveSaveErrorMessage(err: any): string {
    const backendMessage = err?.error?.message;

    if (err?.status === 409) {
      if (typeof backendMessage === 'string' && backendMessage.trim()) {
        return backendMessage;
      }
      return 'Email already exists. Please use a different email address.';
    }

    return backendMessage || 'Failed to save user';
  }

  trackByUser(index: number, user: AdminUser): string {
    return user.id;
  }

  trackByValue(index: number, value: string): string {
    return value;
  }
}


