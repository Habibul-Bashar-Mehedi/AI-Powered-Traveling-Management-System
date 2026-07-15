import { Component } from '@angular/core';
import { Modal } from '../modal/modal';
import { ConfirmDialogService } from './confirm-dialog.service';

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [Modal],
  templateUrl: './confirm-dialog.html',
  styleUrl: './confirm-dialog.css'
})
export class ConfirmDialog {
  constructor(public dialogService: ConfirmDialogService) {}
}
