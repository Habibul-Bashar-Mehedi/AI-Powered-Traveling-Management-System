import { Component, OnInit, OnDestroy, ChangeDetectorRef, NgZone, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  UserServiceRequestPayload,
  UserServiceRequestType,
  UserBookingStatusSummary,
  VendorBookingService
} from '../services/vendor-booking.service';
import { VendorBooking, PublicServiceListing } from '../models/vendor.model';
import { VendorBookingStatus, PaymentMethod, ServiceType } from '../enums/vendor.enums';
import { AuthService } from '../services/auth.service';
import { DestinationService } from '../services/destination.service';
import { Destination } from '../models/destination.model';
import { AiChatService } from '../services/ai-chat.service';
import { ServiceCatalogService } from '../services/service-catalog.service';
import { BannerService } from '../services/banner.service';
import { Banner } from '../models/banner.model';
import { ThemeService } from '../services/theme.service';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription, firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';

interface DashboardStat {
  label: string;
  value: string;
  icon: string;
  trend: string;
  color: 'cyan' | 'violet' | 'amber' | 'rose';
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.css'],
  host: {
    '(document:keydown.escape)': 'onEscapeKey()'
  },
})
export class Dashboard implements OnInit, OnDestroy {
  constructor(
    private cdr: ChangeDetectorRef,
    private ngZone: NgZone,
    private vendorBookingService: VendorBookingService,
    private authService: AuthService,
    private destinationService: DestinationService,
    private aiChatService: AiChatService,
    private serviceCatalogService: ServiceCatalogService,
    private bannerService: BannerService,
    private themeService: ThemeService,
    private route: ActivatedRoute,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  actionLoading: Record<string, boolean> = {};
  actionMessage: Record<string, string> = {};
  actionError: Record<string, string> = {};

  // ─── Banners (admin-managed offers) ────────────────────────────────
  banners: Banner[] = [];
  bannersLoading = false;

  // ─── My Bookings ──────────────────────────────────────────────────
  myBookings: VendorBooking[] = [];
  myBookingsLoading = false;
  showMyBookings = false;
  bookingStatusFilter: VendorBookingStatus | '' = '';
  bookingStatusSummary: UserBookingStatusSummary | null = null;
  bookingCancelLoading: string | null = null;
  bookingStatusOptions = Object.values(VendorBookingStatus);
  private bookingsPollId?: ReturnType<typeof setInterval>;
  private readonly bookingsPollMs = 15000;

  // ─── User Profile ───────────────────────────────────────────────
  user = {
    name: 'Traveler',
    notifications: 0,
  };

  get userInitials(): string {
    const parts = this.user.name.trim().split(/\s+/).filter(Boolean);
    if (parts.length === 0) return 'T';
    return parts.slice(0, 2).map(p => p[0]!.toUpperCase()).join('');
  }

  // ─── Notifications (derived from real booking activity) ───────────
  notificationsOpen = false;

  get notificationItems(): Array<{ id: string; icon: string; text: string; time: string; tone: 'pending' | 'confirmed' | 'rejected' }> {
    return this.allBookings
      .filter(b => b.bookingStatus !== VendorBookingStatus.COMPLETED)
      .map(b => {
        const eventTime = b.confirmedAt || b.completedAt || b.createdAt;
        let text: string;
        let tone: 'pending' | 'confirmed' | 'rejected';
        let icon: string;
        switch (b.bookingStatus) {
          case VendorBookingStatus.CONFIRMED:
            text = `${b.serviceName} was confirmed`;
            tone = 'confirmed';
            icon = '✅';
            break;
          case VendorBookingStatus.REJECTED:
            text = `${b.serviceName} was rejected`;
            tone = 'rejected';
            icon = '⚠️';
            break;
          case VendorBookingStatus.CANCELLED:
            text = `${b.serviceName} booking was cancelled`;
            tone = 'rejected';
            icon = '⚠️';
            break;
          default:
            text = `Awaiting vendor confirmation for ${b.serviceName}`;
            tone = 'pending';
            icon = '⏳';
        }
        return { id: b.bookingId, icon, text, time: this.formatRelativeTime(eventTime), tone, sortKey: new Date(eventTime).getTime() };
      })
      .sort((a, b) => b.sortKey - a.sortKey)
      .slice(0, 8);
  }

  toggleNotifications(): void {
    this.notificationsOpen = !this.notificationsOpen;
    this.accountMenuOpen = false;
    this.cdr.detectChanges();
  }

  get isDarkTheme(): boolean {
    return this.themeService.theme() === 'dark';
  }

  toggleTheme(): void {
    this.themeService.toggleTheme();
  }

  private formatRelativeTime(iso: string): string {
    const diffMs = Date.now() - new Date(iso).getTime();
    const minutes = Math.floor(diffMs / 60000);
    if (minutes < 1) return 'just now';
    if (minutes < 60) return `${minutes}m ago`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours}h ago`;
    const days = Math.floor(hours / 24);
    if (days < 7) return `${days}d ago`;
    return new Date(iso).toLocaleDateString();
  }

  // ─── Account menu ─────────────────────────────────────────────────
  accountMenuOpen = false;

  toggleAccountMenu(): void {
    this.notificationsOpen = false;
    this.accountMenuOpen = !this.accountMenuOpen;
  }

  logout(): void {
    this.accountMenuOpen = false;
    this.authService.logout().subscribe({ complete: () => this.router.navigate(['/login']) });
  }

  // ─── Header date ──────────────────────────────────────────────────
  todayDate = new Date();

  // ─── Stats (derived from real booking data) ───────────────────────
  stats: DashboardStat[] = [];
  private allBookings: VendorBooking[] = [];

  // ─── Upcoming trips (drives hero slider) ───────────────────────────
  upcomingBookings: VendorBooking[] = [];
  private readonly slideThemes = ['slide-theme-blue', 'slide-theme-cyan', 'slide-theme-amber', 'slide-theme-violet'];

  // ─── Destinations ─────────────────────────────────────────────────
  destinations: Destination[] = [];
  destinationsLoading = false;

  // ─── Service catalog (real vendor listings, incl. images) ──────────
  catalogServices: PublicServiceListing[] = [];
  catalogLoading = false;

  // ─── Booking modal (lets a user book more than one unit of a service) ──
  bookingDraft: PublicServiceListing | null = null;
  bookingDraftQuantity = 1;
  bookingDraftNotes = '';
  bookingDraftSubmitting = false;
  bookingDraftError = '';
  private static readonly DEFAULT_MAX_QUANTITY = 20;

  // ─── Payment method (simulated checkout — no live gateway integration) ──
  readonly paymentOptions: Array<{
    value: PaymentMethod;
    label: string;
    icon: string;
    referenceLabel: string;
    referencePlaceholder: string;
    referenceType: 'tel' | 'text';
    referencePattern?: string;
  }> = [
    { value: PaymentMethod.BKASH, label: 'bKash', icon: '📱', referenceLabel: 'bKash Number', referencePlaceholder: '01XXXXXXXXX', referenceType: 'tel', referencePattern: '^01[3-9][0-9]{8}$' },
    { value: PaymentMethod.ROCKET, label: 'Rocket', icon: '🚀', referenceLabel: 'Rocket Number', referencePlaceholder: '01XXXXXXXXX', referenceType: 'tel', referencePattern: '^01[3-9][0-9]{8}$' },
    { value: PaymentMethod.NAGAD, label: 'Nagad', icon: '🟠', referenceLabel: 'Nagad Number', referencePlaceholder: '01XXXXXXXXX', referenceType: 'tel', referencePattern: '^01[3-9][0-9]{8}$' },
    { value: PaymentMethod.BANK_TRANSFER, label: 'Bank Transfer', icon: '🏦', referenceLabel: 'Account Number', referencePlaceholder: 'e.g. 1234567890', referenceType: 'text' },
    { value: PaymentMethod.CARD, label: 'Card', icon: '💳', referenceLabel: 'Card Number', referencePlaceholder: 'e.g. 4111 1111 1111 1111', referenceType: 'text' }
  ];
  bookingDraftPaymentMethod: PaymentMethod | null = null;
  bookingDraftPaymentReference = '';

  // ─── Booking date + live availability (prevents double-booking the same seat/room/day) ──
  bookingDraftStartDate = '';
  bookingDraftEndDate = '';
  bookingDraftAvailability: number | null = null;
  bookingDraftAvailabilityLoading = false;
  bookingDraftAvailabilityError = '';
  private availabilityRequestId = 0;

  // ─── Booking confirmation popup ─────────────────────────────────────
  bookingConfirmation: (VendorBooking & { currencyCode?: string; vendorBusinessName?: string }) | null = null;

  // ─── Active nav item ──────────────────────────────────────────────
  activeNav = 'dashboard';
  navItems = [
    { id: 'dashboard', label: 'Dashboard', icon: '⊞' },
    { id: 'trips', label: 'My Trips', icon: '✈️' },
    { id: 'offers', label: 'Offers', icon: '️' },
    { id: 'profile', label: 'Profile', icon: '' },
  ];

  // ─── Slider ───────────────────────────────────────────────────────
  currentIndex = 0;
  intervalId: ReturnType<typeof setInterval> | undefined;
  private currentUserSubscription?: Subscription;
  private fragmentSubscription?: Subscription;

  ngOnInit() {
    this.startAutoSlide();
    this.loadChatHistory();
    // Load real user name from AuthService
    const currentUser = this.authService.getCurrentUserValue();
    if (currentUser?.username) {
      this.user.name = currentUser.username;
      this.chatUsername = currentUser.username;
    }
    this.currentUserSubscription = this.authService.currentUser$.subscribe(u => {
      if (u?.username) {
        this.user.name = u.username;
        this.chatUsername = u.username;
        this.cdr.detectChanges();
      }
    });
    if (isPlatformBrowser(this.platformId)) {
      this.loadBookingStatusSummary();
      this.loadAllBookingsForStats();
    }
    this.loadDestinations();
    this.loadCatalogServices();
    this.loadBanners();

    // Respond to the nav bar's "My Bookings" / "Packages" links every time they're
    // clicked, not just on first load — a fragment-only navigation while already on
    // /dashboard doesn't re-run ngOnInit, so a snapshot check here would silently do
    // nothing on repeat clicks. Subscribing to the fragment stream fixes that.
    this.fragmentSubscription = this.route.fragment.subscribe(fragment => {
      if (!fragment) return;
      if (fragment === 'my-bookings' && !this.showMyBookings) {
        this.toggleMyBookings();
      }
      if (isPlatformBrowser(this.platformId)) {
        // Wait a tick so conditionally-rendered content (e.g. the just-expanded
        // bookings panel) is in the DOM before we scroll to it.
        setTimeout(() => document.getElementById(fragment)?.scrollIntoView({ behavior: 'smooth', block: 'start' }));
      }
    });
  }

  ngOnDestroy() {
    if (this.intervalId) clearInterval(this.intervalId);
    this.fragmentSubscription?.unsubscribe();
    this.currentUserSubscription?.unsubscribe();
    this.stopBookingsPolling();
  }

  startAutoSlide() {
    this.intervalId = setInterval(() => this.next(), 5000);
  }

  next() {
    const len = this.upcomingBookings.length || 1;
    this.currentIndex = (this.currentIndex + 1) % len;
    this.cdr.detectChanges();
  }

  prev() {
    const len = this.upcomingBookings.length || 1;
    this.currentIndex = (this.currentIndex - 1 + len) % len;
    this.cdr.detectChanges();
  }

  goToSlide(i: number) {
    this.currentIndex = i;
    this.cdr.detectChanges();
  }

  slideTheme(i: number): string {
    return this.slideThemes[i % this.slideThemes.length];
  }

  // ─── Chatbot ──────────────────────────────────────────────────────
  showChatbot = false;
  userInput: string = '';
  isTyping = false;
  messages: { sender: string; text: string; timestamp: Date }[] = [];
  chatMinimized = false;
  private chatUsername: string | null = null;

  toggleChatbot() {
    this.showChatbot = !this.showChatbot;
    if (this.showChatbot && this.messages.length === 0) {
      const name = this.user.name !== 'Traveler' ? this.user.name : 'there';
      this.messages.push({
        sender: 'bot',
        text: `Hello ${name}! 👋 I'm your AI Travel Assistant. Tell me your budget and I'll recommend the best tour package for you from what's available right now — I can also help with destinations, deals, and itineraries. What would you like to explore today?`,
        timestamp: new Date(),
      });
    }
    this.chatMinimized = false;
    this.cdr.detectChanges();
    this.scrollToBottom();
  }

  async sendMessage() {
    if (!this.userInput.trim() || this.isTyping) return;
    const prompt = this.userInput.trim();
    this.messages.push({ sender: 'user', text: prompt, timestamp: new Date() });
    this.userInput = '';
    this.isTyping = true;
    this.cdr.detectChanges();
    this.scrollToBottom();

    try {
      const history = this.messages
        .slice(-8)
        .map(m => ({ sender: m.sender, text: m.text }));

      const data = await firstValueFrom(this.aiChatService.sendMessage({
        username: this.chatUsername,
        message: prompt,
        history,
      }));
      const botText = data.reply || 'Sorry, please try again.';
      // The fetch-backed HttpClient can resolve this promise outside Angular's zone,
      // so pushing state and scheduling change detection must be forced back into it —
      // otherwise the reply never appears until some unrelated event happens to trigger CD.
      this.ngZone.run(() => {
        this.messages.push({ sender: 'bot', text: botText, timestamp: new Date() });
        this.saveChatHistory();
      });
    } catch {
      this.ngZone.run(() => {
        this.messages.push({ sender: 'bot', text: '⚠️ Connection error. Please check your network and try again.', timestamp: new Date() });
      });
    } finally {
      this.ngZone.run(() => {
        this.isTyping = false;
        this.cdr.detectChanges();
        this.scrollToBottom();
      });
    }
  }

  minimizeChat() {
    this.chatMinimized = !this.chatMinimized;
    this.cdr.detectChanges();
  }

  clearChat() {
    if (confirm('Delete all chat history?')) {
      this.messages = [];
      if (isPlatformBrowser(this.platformId)) localStorage.removeItem('chatHist');
      this.messages.push({
        sender: 'bot',
        text: '️ Chat cleared. Ready to help you plan your next adventure!',
        timestamp: new Date(),
      });
      this.cdr.detectChanges();
    }
  }

  onEnterPress(event: any) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  saveChatHistory() {
    if (isPlatformBrowser(this.platformId)) localStorage.setItem('chatHist', JSON.stringify(this.messages));
  }

  loadChatHistory() {
    if (isPlatformBrowser(this.platformId)) {
      const saved = localStorage.getItem('chatHist');
      if (saved) {
        try {
          this.messages = JSON.parse(saved);
        } catch {
          // Corrupted data — start fresh
          localStorage.removeItem('chatHist');
          this.messages = [];
        }
      }
    }
  }

  formatTimestamp(date: any) {
    return new Date(date).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }

  scrollToBottom() {
    setTimeout(() => {
      const el = document.querySelector('.chat-body');
      if (el) el.scrollTop = el.scrollHeight;
    }, 100);
  }

  setActiveNav(id: string) {
    this.activeNav = id;
  }

  onDestinationAction(dest: Destination): void {
    this.submitServiceRequest('EXPLORE_TOURIST_PLACES', dest.name, `Exploration request for ${dest.name}, ${dest.region}.`);
  }

  // ─── Destinations ────────────────────────────────────────────────
  loadDestinations(): void {
    this.destinationsLoading = true;
    this.destinationService.getAll().subscribe({
      next: (destinations) => {
        this.destinations = destinations;
        this.destinationsLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.destinationsLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  // ─── Service catalog ─────────────────────────────────────────────
  loadCatalogServices(): void {
    this.catalogLoading = true;
    this.serviceCatalogService.getActiveServices().subscribe({
      next: (page) => {
        this.catalogServices = page.content;
        this.catalogLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.catalogLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  // ─── Booking modal (choose date, quantity, then confirm) ────────────────
  get bookingDraftIsHotel(): boolean {
    return this.bookingDraft?.serviceType === ServiceType.HOTEL_ROOM;
  }

  get minBookingDate(): string {
    return new Date().toISOString().slice(0, 10);
  }

  get bookingDraftMaxQuantity(): number {
    const serviceMax = this.bookingDraft?.maxCapacity && this.bookingDraft.maxCapacity > 0
      ? this.bookingDraft.maxCapacity
      : Dashboard.DEFAULT_MAX_QUANTITY;
    return this.bookingDraftAvailability !== null
      ? Math.min(serviceMax, this.bookingDraftAvailability)
      : serviceMax;
  }

  get bookingDraftUnitLabel(): string {
    switch (this.bookingDraft?.pricingUnit) {
      case 'PER_NIGHT': return 'Night(s)';
      case 'PER_PERSON': return 'Traveler(s)';
      case 'PER_SEAT': return 'Seat(s)';
      case 'PER_TRIP': return 'Trip(s)';
      default: return 'Unit(s)';
    }
  }

  get bookingDraftTotal(): number {
    return (this.bookingDraft?.basePrice ?? 0) * this.bookingDraftQuantity;
  }

  get isBookingDateValid(): boolean {
    if (!this.bookingDraftStartDate) return false;
    if (this.bookingDraftIsHotel) {
      return !!this.bookingDraftEndDate && this.bookingDraftEndDate >= this.bookingDraftStartDate;
    }
    return true;
  }

  openBookingModal(service: PublicServiceListing): void {
    this.bookingDraft = service;
    this.bookingDraftQuantity = 1;
    this.bookingDraftNotes = '';
    this.bookingDraftError = '';
    this.bookingDraftSubmitting = false;
    this.bookingDraftPaymentMethod = null;
    this.bookingDraftPaymentReference = '';
    this.bookingDraftAvailability = null;
    this.bookingDraftAvailabilityError = '';
    this.bookingDraftStartDate = this.minBookingDate;
    this.bookingDraftEndDate = '';
    this.onBookingDateChange();
  }

  closeBookingModal(): void {
    if (this.bookingDraftSubmitting) return;
    this.bookingDraft = null;
  }

  onBookingDateChange(): void {
    this.bookingDraftError = '';
    if (!this.isBookingDateValid) {
      this.bookingDraftAvailability = null;
      return;
    }
    this.refreshAvailability();
  }

  private refreshAvailability(): void {
    const service = this.bookingDraft;
    if (!service || !this.isBookingDateValid) return;

    const requestId = ++this.availabilityRequestId;
    this.bookingDraftAvailabilityLoading = true;
    this.bookingDraftAvailabilityError = '';
    this.cdr.detectChanges();

    const endDate = this.bookingDraftIsHotel ? this.bookingDraftEndDate : undefined;
    this.serviceCatalogService.getAvailability(service.serviceId, this.bookingDraftStartDate, endDate).subscribe({
      next: (res) => {
        if (requestId !== this.availabilityRequestId) return; // a newer date change superseded this response
        this.bookingDraftAvailabilityLoading = false;
        this.bookingDraftAvailability = res.available;
        this.setBookingDraftQuantity(this.bookingDraftQuantity);
        this.cdr.detectChanges();
      },
      error: (err) => {
        if (requestId !== this.availabilityRequestId) return;
        this.bookingDraftAvailabilityLoading = false;
        this.bookingDraftAvailability = null;
        this.bookingDraftAvailabilityError = err?.error?.message || 'Could not check availability for this date.';
        this.cdr.detectChanges();
      }
    });
  }

  decrementBookingQuantity(): void {
    this.setBookingDraftQuantity(this.bookingDraftQuantity - 1);
  }

  incrementBookingQuantity(): void {
    this.setBookingDraftQuantity(this.bookingDraftQuantity + 1);
  }

  setBookingDraftQuantity(value: number): void {
    const max = Math.max(this.bookingDraftMaxQuantity, 0);
    if (!Number.isFinite(value)) value = 1;
    this.bookingDraftQuantity = max === 0 ? 0 : Math.min(Math.max(Math.round(value), 1), max);
  }

  get selectedPaymentOption() {
    return this.paymentOptions.find(o => o.value === this.bookingDraftPaymentMethod) ?? null;
  }

  selectPaymentMethod(method: PaymentMethod): void {
    if (this.bookingDraftPaymentMethod !== method) {
      this.bookingDraftPaymentReference = '';
    }
    this.bookingDraftPaymentMethod = method;
    this.bookingDraftError = '';
  }

  get isPaymentReferenceValid(): boolean {
    const option = this.selectedPaymentOption;
    if (!option) return false;
    const value = this.bookingDraftPaymentReference.trim();
    if (!value) return false;
    return option.referencePattern ? new RegExp(option.referencePattern).test(value) : true;
  }

  get canConfirmBooking(): boolean {
    return this.isBookingDateValid
      && !this.bookingDraftAvailabilityLoading
      && this.bookingDraftAvailability !== null
      && this.bookingDraftAvailability > 0
      && this.bookingDraftQuantity > 0
      && this.bookingDraftQuantity <= this.bookingDraftAvailability
      && !!this.bookingDraftPaymentMethod
      && this.isPaymentReferenceValid
      && !this.bookingDraftSubmitting;
  }

  confirmBooking(): void {
    const service = this.bookingDraft;
    if (!service || this.bookingDraftSubmitting) return;

    if (!this.isBookingDateValid) {
      this.bookingDraftError = this.bookingDraftIsHotel
        ? 'Please select a valid check-in and check-out date.'
        : 'Please select a booking date.';
      return;
    }
    if (this.bookingDraftAvailability === null) {
      this.bookingDraftError = 'Still checking availability for this date — please wait a moment.';
      return;
    }
    if (this.bookingDraftAvailability <= 0) {
      this.bookingDraftError = 'This service is fully booked for the selected date(s). Try another date.';
      return;
    }
    if (this.bookingDraftQuantity > this.bookingDraftAvailability) {
      this.bookingDraftError = `Only ${this.bookingDraftAvailability} left for the selected date(s).`;
      return;
    }
    if (!this.bookingDraftPaymentMethod) {
      this.bookingDraftError = 'Please select a payment method.';
      return;
    }
    if (!this.isPaymentReferenceValid) {
      this.bookingDraftError = `Enter a valid ${this.selectedPaymentOption?.referenceLabel.toLowerCase()}.`;
      return;
    }

    this.bookingDraftSubmitting = true;
    this.bookingDraftError = '';
    this.cdr.detectChanges();

    this.serviceCatalogService.bookService(service.serviceId, {
      startDate: this.bookingDraftStartDate,
      endDate: this.bookingDraftIsHotel ? this.bookingDraftEndDate : undefined,
      quantity: this.bookingDraftQuantity,
      specialRequests: this.bookingDraftNotes.trim() || undefined,
      paymentMethod: this.bookingDraftPaymentMethod,
      paymentReference: this.bookingDraftPaymentReference.trim()
    }).subscribe({
      next: (booking) => {
        this.bookingDraftSubmitting = false;
        this.bookingDraft = null;
        this.bookingConfirmation = {
          ...booking,
          currencyCode: service.currencyCode,
          vendorBusinessName: service.vendorBusinessName
        };
        this.refreshBookingsAfterMutation();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.bookingDraftSubmitting = false;
        this.bookingDraftError = err?.error?.message || 'Failed to book this service. Please try again.';
        // Someone else may have booked the remaining capacity between our last check and
        // this submit — re-check so the quantity cap on screen reflects reality.
        this.refreshAvailability();
        this.cdr.detectChanges();
      }
    });
  }

  closeBookingConfirmation(): void {
    this.bookingConfirmation = null;
  }

  onEscapeKey(): void {
    if (this.bookingConfirmation) {
      this.closeBookingConfirmation();
    } else if (this.bookingDraft) {
      this.closeBookingModal();
    }
  }

  viewMyBookingsFromConfirmation(): void {
    this.bookingConfirmation = null;
    if (!this.showMyBookings) {
      this.toggleMyBookings();
    }
    if (!isPlatformBrowser(this.platformId)) return;
    document.getElementById('my-bookings')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  resolveImageUrl(url: string | null | undefined): string {
    if (!url) return '';
    if (/^https?:\/\//i.test(url)) return url;
    return `${environment.assetOrigin}${url}`;
  }

  // ─── Banners (admin-managed offers) ────────────────────────────
  loadBanners(): void {
    this.bannersLoading = true;
    this.bannerService.getActiveBanners().subscribe({
      next: (banners) => {
        this.banners = banners || [];
        this.bannersLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.bannersLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  bannerCtaClick(banner: Banner): void {
    const target = banner.ctaTarget || 'offers';
    if (/^https?:\/\//i.test(target)) {
      if (isPlatformBrowser(this.platformId)) window.open(target, '_blank', 'noopener');
      return;
    }
    if (!isPlatformBrowser(this.platformId)) return;
    document.getElementById(target)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  // ─── My Bookings ───────────────────────────────────────────────
  loadBookingStatusSummary(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    this.vendorBookingService.getMyBookingStatusSummary().subscribe({
      next: (summary) => {
        this.bookingStatusSummary = summary;
        this.user.notifications = summary.counts?.PENDING ?? 0;
        this.buildStats();
        this.cdr.detectChanges();
      },
      error: () => {
        // Non-critical — dashboard still works without summary counts
      }
    });
  }

  private loadAllBookingsForStats(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    this.vendorBookingService.getMyBookings().subscribe({
      next: (bookings) => {
        this.allBookings = bookings;
        this.buildUpcomingBookings();
        this.buildStats();
        this.cdr.detectChanges();
      },
      error: () => {
        // Non-critical — dashboard still works without stats/upcoming trips
      }
    });
  }

  private buildUpcomingBookings(): void {
    this.upcomingBookings = this.allBookings
      .filter(b => b.bookingStatus === VendorBookingStatus.PENDING || b.bookingStatus === VendorBookingStatus.CONFIRMED)
      .sort((a, b) => new Date(a.startDate).getTime() - new Date(b.startDate).getTime())
      .slice(0, 4);
    this.currentIndex = 0;
  }

  private buildStats(): void {
    const counts = this.bookingStatusSummary?.counts ?? {};
    const total = this.bookingStatusSummary?.total ?? 0;
    const paidBookings = this.allBookings.filter(
      b => b.bookingStatus === VendorBookingStatus.CONFIRMED || b.bookingStatus === VendorBookingStatus.COMPLETED
    );
    const totalSpent = paidBookings.reduce((sum, b) => sum + (b.grossAmount || 0), 0);

    this.stats = [
      {
        label: 'Total Requests',
        value: String(total),
        icon: '📋',
        trend: `${counts[VendorBookingStatus.PENDING] ?? 0} pending`,
        color: 'cyan'
      },
      {
        label: 'Confirmed Trips',
        value: String(counts[VendorBookingStatus.CONFIRMED] ?? 0),
        icon: '✅',
        trend: `${counts[VendorBookingStatus.COMPLETED] ?? 0} completed`,
        color: 'violet'
      },
      {
        label: 'Completed Trips',
        value: String(counts[VendorBookingStatus.COMPLETED] ?? 0),
        icon: '🏁',
        trend: `${(counts[VendorBookingStatus.CANCELLED] ?? 0) + (counts[VendorBookingStatus.REJECTED] ?? 0)} cancelled/rejected`,
        color: 'amber'
      },
      {
        label: 'Total Spent',
        value: `৳${totalSpent.toLocaleString()}`,
        icon: '💳',
        trend: `${paidBookings.length} paid booking${paidBookings.length === 1 ? '' : 's'}`,
        color: 'rose'
      }
    ];
  }

  loadMyBookings(silent = false): void {
    if (!isPlatformBrowser(this.platformId)) return;
    if (!silent) {
      this.myBookingsLoading = true;
      this.cdr.detectChanges();
    }
    const status = this.bookingStatusFilter
      ? (this.bookingStatusFilter as VendorBookingStatus)
      : undefined;

    this.vendorBookingService.getMyBookings(status).subscribe({
      next: (bookings) => {
        this.myBookings = bookings;
        this.myBookingsLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.myBookingsLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  toggleMyBookings(): void {
    this.showMyBookings = !this.showMyBookings;
    this.cdr.detectChanges();
    if (this.showMyBookings) {
      this.loadMyBookings();
      this.startBookingsPolling();
      return;
    }
    this.stopBookingsPolling();
    // Clear the #my-bookings fragment so a page reload doesn't silently re-expand
    // the panel via the ngOnInit deep-link check below.
    this.router.navigate([], { relativeTo: this.route, fragment: undefined, replaceUrl: true });
  }

  onBookingStatusFilterChange(): void {
    this.loadMyBookings();
  }

  onStatusChipClick(status: VendorBookingStatus): void {
    this.bookingStatusFilter = this.bookingStatusFilter === status ? '' : status;
    this.showMyBookings = true;
    this.cdr.detectChanges();
    this.loadMyBookings();
    if (!this.bookingsPollId) {
      this.startBookingsPolling();
    }
  }

  statusCount(status: VendorBookingStatus): number {
    return this.bookingStatusSummary?.counts?.[status] ?? 0;
  }

  canCancelBooking(status: VendorBookingStatus): boolean {
    return status === VendorBookingStatus.PENDING || status === VendorBookingStatus.CONFIRMED;
  }

  cancelMyBooking(booking: VendorBooking): void {
    const reason = 'Cancelled from user dashboard';
    this.bookingCancelLoading = booking.bookingId;
    this.vendorBookingService.cancelMyBooking(booking.bookingId, reason).subscribe({
      next: () => {
        this.bookingCancelLoading = null;
        this.refreshBookingsAfterMutation();
        this.cdr.detectChanges();
      },
      error: () => {
        this.bookingCancelLoading = null;
        this.cdr.detectChanges();
      }
    });
  }

  bookingStatusLabel(status: string): string {
    return status.charAt(0) + status.slice(1).toLowerCase();
  }

  paymentMethodDisplay(method: string | undefined | null): string {
    if (!method) return '—';
    const option = this.paymentOptions.find(o => o.value === method);
    return option ? `${option.icon} ${option.label}` : method;
  }

  packageItemIcon(itemType: string): string {
    switch (itemType) {
      case 'TRANSPORT': return '🚌';
      case 'HOTEL': return '🏨';
      case 'ACTIVITY': return '🎟️';
      case 'MEAL': return '🍽️';
      default: return '📍';
    }
  }

  bookingStatusClass(status: string): string {
    switch (status) {
      case VendorBookingStatus.CONFIRMED:  return 'badge-confirmed';
      case VendorBookingStatus.PENDING:    return 'badge-pending';
      case VendorBookingStatus.COMPLETED:  return 'badge-completed';
      case VendorBookingStatus.CANCELLED:  return 'badge-cancelled';
      case VendorBookingStatus.REJECTED:   return 'badge-rejected';
      default:           return 'badge-pending';
    }
  }

  refreshBookings(): void {
    this.refreshBookingsAfterMutation();
  }

  private refreshBookingsAfterMutation(): void {
    this.loadBookingStatusSummary();
    this.loadAllBookingsForStats();
    if (this.showMyBookings) {
      this.loadMyBookings(true);
    }
  }

  private startBookingsPolling(): void {
    this.stopBookingsPolling();
    if (!isPlatformBrowser(this.platformId)) return;
    this.bookingsPollId = setInterval(() => {
      if (this.showMyBookings) {
        this.loadMyBookings(true);
        this.loadBookingStatusSummary();
      }
    }, this.bookingsPollMs);
  }

  private stopBookingsPolling(): void {
    if (this.bookingsPollId) {
      clearInterval(this.bookingsPollId);
      this.bookingsPollId = undefined;
    }
  }

  private submitServiceRequest(requestType: UserServiceRequestType, key: string, note: string): void {
    const payload: UserServiceRequestPayload = {
      requestType,
      title: key,
      quantity: 1,
      specialRequests: note
    };

    this.actionLoading[key] = true;
    this.actionMessage[key] = '';
    this.actionError[key] = '';

    this.vendorBookingService.createUserServiceRequest(payload).subscribe({
      next: () => {
        this.actionLoading[key] = false;
        this.actionMessage[key] = 'Request sent. Vendor will review and respond from the vendor dashboard.';
        this.refreshBookingsAfterMutation();
        if (!this.bookingsPollId) {
          this.startBookingsPolling();
        }
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.actionLoading[key] = false;
        this.actionError[key] = err?.error?.message || 'Could not submit request right now.';
        this.cdr.detectChanges();
      }
    });
  }
}
