import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { Password } from 'primeng/password';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { UserProfile } from '../../core/auth/auth.models';
import { AuthStore } from '../../core/auth/auth.store';
import { WorkspaceApi } from '../../core/api/workspace.api';
import { PASSWORD_MESSAGE, PASSWORD_PATTERN, apiMessage, applyApiErrors, controlError } from '../../core/forms/form-errors';

interface Option<T extends string> {
  label: string;
  value: T;
}

@Component({
  selector: 'app-team-page',
  imports: [DatePipe, ReactiveFormsModule, TableModule, Button, Dialog, InputText, Password, Select, Tag],
  templateUrl: './team-page.html',
})
export class TeamPage {
  private readonly api = inject(WorkspaceApi);
  private readonly builder = inject(FormBuilder);
  readonly store = inject(AuthStore);
  readonly people = signal<UserProfile[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly dialogError = signal<string | null>(null);
  readonly saving = signal(false);
  readonly dialogOpen = signal(false);
  readonly editing = signal<UserProfile | null>(null);
  readonly passwordMessage = PASSWORD_MESSAGE;
  readonly statuses: Option<UserProfile['status']>[] = [
    { label: 'Active', value: 'ACTIVE' },
    { label: 'Disabled', value: 'DISABLED' },
  ];
  readonly roleChoices: Option<UserProfile['roleCode']>[] = [
    { label: 'Owner', value: 'OWNER' },
    { label: 'Admin', value: 'ADMIN' },
    { label: 'Analyst', value: 'ANALYST' },
    { label: 'Viewer', value: 'VIEWER' },
  ];
  readonly assignableRoles = computed(() => {
    const actor = this.store.user();
    const target = this.editing();
    return this.roleChoices.filter((role) => {
      if (role.value !== 'OWNER') return true;
      if (actor?.roleCode === 'OWNER') return true;
      return target?.roleCode === 'OWNER';
    });
  });
  readonly form = this.builder.nonNullable.group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(320)]],
    firstName: ['', [Validators.required, Validators.maxLength(120)]],
    lastName: ['', [Validators.required, Validators.maxLength(120)]],
    password: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(128), Validators.pattern(PASSWORD_PATTERN)]],
    roleCode: this.builder.nonNullable.control<UserProfile['roleCode']>('ANALYST', Validators.required),
    status: this.builder.nonNullable.control<UserProfile['status']>('ACTIVE', Validators.required),
  });

  constructor() {
    this.load();
  }

  canWrite(): boolean {
    return this.store.has('users:write');
  }

  isSelf(person: UserProfile | null): boolean {
    return !!person && person.id === this.store.user()?.id;
  }

  canEdit(person: UserProfile): boolean {
    if (!this.canWrite()) return false;
    if (person.roleCode === 'OWNER' && this.store.user()?.roleCode !== 'OWNER') return false;
    return true;
  }

  fieldError(name: 'email' | 'firstName' | 'lastName' | 'password' | 'roleCode' | 'status'): string | null {
    return controlError(this.form.controls[name], name === 'password' ? PASSWORD_MESSAGE : undefined);
  }

  openCreate(): void {
    this.editing.set(null);
    this.dialogError.set(null);
    this.form.reset({
      email: '',
      firstName: '',
      lastName: '',
      password: '',
      roleCode: 'ANALYST',
      status: 'ACTIVE',
    });
    this.form.controls.email.enable();
    this.form.controls.password.enable();
    this.form.controls.roleCode.enable();
    this.form.controls.status.enable();
    this.dialogOpen.set(true);
  }

  openEdit(person: UserProfile): void {
    this.editing.set(person);
    this.dialogError.set(null);
    this.form.reset({
      email: person.email,
      firstName: person.firstName,
      lastName: person.lastName,
      password: '',
      roleCode: person.roleCode,
      status: person.status,
    });
    this.form.controls.email.disable();
    this.form.controls.password.disable();
    if (this.isSelf(person)) {
      this.form.controls.roleCode.disable();
      this.form.controls.status.disable();
    } else {
      this.form.controls.roleCode.enable();
      this.form.controls.status.enable();
    }
    this.dialogOpen.set(true);
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.dialogError.set(null);
    const value = this.form.getRawValue();
    const editing = this.editing();
    const request = editing
      ? this.api.updateUser(editing.id, {
          firstName: value.firstName.trim(),
          lastName: value.lastName.trim(),
          roleCode: value.roleCode,
          status: value.status,
        })
      : this.api.createUser({
          email: value.email.trim(),
          firstName: value.firstName.trim(),
          lastName: value.lastName.trim(),
          password: value.password,
          roleCode: value.roleCode,
        });
    request.subscribe({
      next: (person) => {
        this.people.update((people) => {
          const index = people.findIndex((item) => item.id === person.id);
          if (index === -1) return [...people, person];
          return people.map((item) => (item.id === person.id ? person : item));
        });
        if (person.id === this.store.user()?.id) {
          this.store.setUser(person);
        }
        this.saving.set(false);
        this.dialogOpen.set(false);
      },
      error: (error: unknown) => {
        this.dialogError.set(applyApiErrors(this.form, error));
        this.saving.set(false);
      },
    });
  }

  private load(): void {
    this.api.users().subscribe({
      next: (people) => {
        this.people.set(people);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.error.set(apiMessage(error));
        this.loading.set(false);
      },
    });
  }
}
