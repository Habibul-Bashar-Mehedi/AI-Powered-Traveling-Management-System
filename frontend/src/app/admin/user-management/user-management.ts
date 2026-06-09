import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { AdminManagementService } from '../../services/admin-management.service';
import { AdminUser, AdminUserRequest } from '../../models/admin-management.model';
import { UserRole, UserRoleLabels } from '../../enums/user-role.enum';

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './user-management.html',
  styleUrls: ['./user-management.css']
})
export class UserManagement implements OnInit {
  users: AdminUser[] = [];
  loading = true;
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

  constructor(
    private adminManagementService: AdminManagementService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(resetPage = false): void {
    if (resetPage) {
      this.page = 0;
    }

    this.loading = true;
    this.error = '';

    this.adminManagementService.getUsers(this.page, this.size, this.search, this.roleFilter).subscribe({
      next: (res) => {
        this.users = res.content;
        this.totalPages = res.totalPages;
        this.totalElements = res.totalElements;
        this.loading = false;
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to load users';
        this.loading = false;
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
      countryId: user.countryId || ''
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
      countryId: this.form.countryId?.trim() || undefined
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

  deleteUser(user: AdminUser): void {
    const ok = window.confirm(`Delete user ${user.email}? This cannot be undone.`);
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
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to delete user';
        this.deletingId = null;
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
      countryId: ''
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
}


