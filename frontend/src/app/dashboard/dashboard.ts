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
import { VendorBookingStatus } from '../enums/vendor.enums';
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

  quickActions: Array<{ label: string; requestType: UserServiceRequestType; note: string }> = [
    {
      label: 'Hotel Booking',
      requestType: 'HOTEL_BOOKING',
      note: 'Need a hotel room booking from dashboard.'
    },
    {
      label: 'Explore Tourist Places',
      requestType: 'EXPLORE_TOURIST_PLACES',
      note: 'Need guided support for exploring tourist places.'
    },
    {
      label: 'Order Traditional Food & Items',
      requestType: 'ORDER_TRADITIONAL_FOOD_ITEMS',
      note: 'Need traditional food and item order assistance.'
    }
  ];

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
  catalogBookingLoading: Record<string, boolean> = {};
  catalogBookingMessage: Record<string, string> = {};
  catalogBookingError: Record<string, string> = {};

  // ─── Active nav item ──────────────────────────────────────────────
  activeNav = 'dashboard';
  navItems = [
    { id: 'dashboard', label: 'Dashboard', icon: '⊞' },
    { id: 'trips', label: 'My Trips', icon: '✈️' },
    { id: 'explore', label: 'Explore', icon: '' },
    { id: 'offers', label: 'Offers', icon: '️' },
    { id: 'profile', label: 'Profile', icon: '' },
  ];

  // ─── Slider ───────────────────────────────────────────────────────
  currentIndex = 0;
  intervalId: ReturnType<typeof setInterval> | undefined;
  private currentUserSubscription?: Subscription;

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

    // Auto-expand the bookings panel when arriving via the "My Bookings" nav link
    if (this.route.snapshot.fragment === 'my-bookings' && !this.showMyBookings) {
      this.toggleMyBookings();
    }
  }

  ngOnDestroy() {
    if (this.intervalId) clearInterval(this.intervalId);
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

  onQuickActionClick(action: { label: string; requestType: UserServiceRequestType; note: string }): void {
    this.submitServiceRequest(action.requestType, action.label, action.note);
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

  bookCatalogService(service: PublicServiceListing): void {
    this.catalogBookingLoading[service.serviceId] = true;
    this.catalogBookingMessage[service.serviceId] = '';
    this.catalogBookingError[service.serviceId] = '';
    this.cdr.detectChanges();

    this.serviceCatalogService.bookService(service.serviceId, { quantity: 1 }).subscribe({
      next: () => {
        this.catalogBookingLoading[service.serviceId] = false;
        this.catalogBookingMessage[service.serviceId] = 'Booked! Check My Bookings for status.';
        this.refreshBookingsAfterMutation();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.catalogBookingLoading[service.serviceId] = false;
        this.catalogBookingError[service.serviceId] = err?.error?.message || 'Failed to book this service.';
        this.cdr.detectChanges();
      }
    });
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
        this.showMyBookings = true;
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
