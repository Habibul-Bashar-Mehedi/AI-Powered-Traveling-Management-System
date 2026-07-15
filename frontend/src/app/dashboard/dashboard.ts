import { Component, OnInit, OnDestroy, ChangeDetectorRef, NgZone, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  UserBookingStatusSummary,
  VendorBookingService
} from '../services/vendor-booking.service';
import { VendorBooking, PublicServiceListing } from '../models/vendor.model';
import { VendorBookingStatus, ServiceType } from '../enums/vendor.enums';
import { AuthService } from '../services/auth.service';
import { AiChatService } from '../services/ai-chat.service';
import { ServiceCatalogService } from '../services/service-catalog.service';
import { BannerService } from '../services/banner.service';
import { Banner } from '../models/banner.model';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subscription, firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { Card } from '../shared/card/card';
import { Modal } from '../shared/modal/modal';
import { ServiceBookingModal } from '../shared/service-booking-modal/service-booking-modal';
import { PayNowModal } from '../shared/pay-now-modal/pay-now-modal';
import { PaymentBookingType } from '../models/payment.model';
import { PackageService } from '../services/package.service';
import { PackageBookRequest, TravelPackage } from '../models/package.model';
import { ConfirmDialogService } from '../shared/confirm-dialog/confirm-dialog.service';
import { LoadingState } from '../shared/loading-state/loading-state';

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
  imports: [FormsModule, CommonModule, RouterLink, Card, Modal, ServiceBookingModal, PayNowModal, LoadingState],
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
    private aiChatService: AiChatService,
    private serviceCatalogService: ServiceCatalogService,
    private packageService: PackageService,
    private bannerService: BannerService,
    private route: ActivatedRoute,
    private confirmDialog: ConfirmDialogService,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}


  // ─── Banners (admin-managed offers) ────────────────────────────────
  banners: Banner[] = [];
  bannersLoading = false;

  // ─── Booking summary (feeds stats cards) ────────────────────────────
  bookingStatusSummary: UserBookingStatusSummary | null = null;

  // ─── User Profile ───────────────────────────────────────────────
  user = {
    name: 'Traveler',
  };

  // ─── Header date ──────────────────────────────────────────────────
  todayDate = new Date();

  // ─── Stats (derived from real booking data) ───────────────────────
  stats: DashboardStat[] = [];
  private allBookings: VendorBooking[] = [];

  // ─── Upcoming trips (drives hero slider) ───────────────────────────
  upcomingBookings: VendorBooking[] = [];
  private readonly slideThemes = ['slide-theme-blue', 'slide-theme-cyan', 'slide-theme-amber', 'slide-theme-violet'];

  // ─── Service catalog (real vendor listings, incl. images) ──────────
  catalogServices: PublicServiceListing[] = [];
  catalogLoading = false;

  // ─── Service catalog grouped into category tiles — selecting one filters
  // the grid below to just that ServiceType instead of showing everything flat ──
  selectedCategory: ServiceType | null = null;

  private static readonly CATEGORY_META: Record<ServiceType, { label: string; icon: string; color: string }> = {
    [ServiceType.HOTEL_ROOM]: { label: 'Hotels', icon: '🏨', color: 'cyan' },
    [ServiceType.TOUR_PACKAGE]: { label: 'Tour Packages', icon: '🧭', color: 'violet' },
    [ServiceType.TRANSPORT_ROUTE]: { label: 'Transport', icon: '🚌', color: 'amber' },
    [ServiceType.TOURIST_SPOT]: { label: 'Tourist Spots', icon: '🗺️', color: 'rose' },
    [ServiceType.TRADITIONAL_FOOD]: { label: 'Traditional Food', icon: '🍽️', color: 'emerald' },
    [ServiceType.TRADITIONAL_ITEM]: { label: 'Traditional Items', icon: '🛍️', color: 'cyan' },
    [ServiceType.MARKET]: { label: 'Markets', icon: '🏪', color: 'violet' },
    [ServiceType.DESTINATION]: { label: 'Destinations', icon: '📍', color: 'amber' },
    [ServiceType.TRAVEL_PACKAGE]: { label: 'Travel Packages', icon: '📦', color: 'rose' },
  };

  get serviceCategories(): { type: ServiceType; label: string; icon: string; color: string; count: number }[] {
    const counts = new Map<ServiceType, number>();
    for (const s of this.catalogServices) {
      counts.set(s.serviceType, (counts.get(s.serviceType) || 0) + 1);
    }
    return Array.from(counts.entries()).map(([type, count]) => ({
      type,
      count,
      ...Dashboard.CATEGORY_META[type]
    }));
  }

  get filteredCatalogServices(): PublicServiceListing[] {
    if (!this.selectedCategory) return this.catalogServices;
    return this.catalogServices.filter(s => s.serviceType === this.selectedCategory);
  }

  get selectedCategoryLabel(): string {
    return this.selectedCategory ? Dashboard.CATEGORY_META[this.selectedCategory].label : '';
  }

  selectCategory(type: ServiceType): void {
    this.selectedCategory = type;
  }

  clearCategoryFilter(): void {
    this.selectedCategory = null;
  }

  // ─── Booking modal (extracted to shared ServiceBookingModal — see openBookingModal) ──
  bookingDraft: PublicServiceListing | null = null;

  // ─── Pay Now modal (opened right after a booking/package is reserved, unpaid) ──
  payNowDraft: {
    bookingType: PaymentBookingType;
    bookingId: string;
    bookingReference: string;
    merchantName: string;
    amount: number;
    currencyCode: string;
  } | null = null;

  // ─── View Details modals (surface fields already present in the list payload) ──
  detailService: PublicServiceListing | null = null;
  detailPackage: TravelPackage | null = null;

  // ─── Travel packages (admin-curated bundles of real bookable services) ─────
  packages: TravelPackage[] = [];
  packagesLoading = false;

  // ─── Package booking modal (one package = one bundle, no quantity picker) ──
  packageBookingDraft: TravelPackage | null = null;
  packageBookingDraftStartDate = '';
  packageBookingDraftSubmitting = false;
  packageBookingDraftError = '';

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
  // Guards against a fast double-click/rapid-navigation destroying this
  // component while one of the load*() calls below is still in flight — without
  // this, the late response's callback fires on a torn-down view and throws when
  // it calls detectChanges(), which can leave a section's *Loading flag stuck.
  private loadSubscriptions = new Subscription();

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
        // No manual detectChanges() here: a BehaviorSubject replays its current value
        // synchronously on subscribe, so this first emission runs nested inside
        // ngOnInit — i.e. still inside Angular's own initial view-creation check.
        // Forcing a recursive detectChanges() from there caused NG0100 elsewhere in
        // this same view once other async data (e.g. notification counts) arrived.
      }
    });
    if (isPlatformBrowser(this.platformId)) {
      this.loadBookingStatusSummary();
      this.loadAllBookingsForStats();
      // These three also require an auth token, unavailable during SSR — same
      // reason as above. Running them unguarded during SSR previously left this
      // data stuck empty after hydration (ngOnInit never re-runs client-side),
      // exactly like the admin dashboard's missing-guard bug.
      this.loadCatalogServices();
      this.loadPackages();
      this.loadBanners();
    }

    // Respond to the nav bar's "Packages" link every time it's clicked, not just
    // on first load — a fragment-only navigation while already on /dashboard
    // doesn't re-run ngOnInit, so a snapshot check here would silently do nothing
    // on repeat clicks. Subscribing to the fragment stream fixes that.
    this.fragmentSubscription = this.route.fragment.subscribe(fragment => {
      if (!fragment) return;
      if (isPlatformBrowser(this.platformId)) {
        setTimeout(() => document.getElementById(fragment)?.scrollIntoView({ behavior: 'smooth', block: 'start' }));
      }
    });
  }

  ngOnDestroy() {
    if (this.intervalId) clearInterval(this.intervalId);
    this.fragmentSubscription?.unsubscribe();
    this.currentUserSubscription?.unsubscribe();
    this.loadSubscriptions.unsubscribe();
  }

  startAutoSlide() {
    this.intervalId = setInterval(() => this.next(), 5000);
  }

  next() {
    const len = this.upcomingBookings.length || 1;
    this.currentIndex = (this.currentIndex + 1) % len;
    // No manual detectChanges() here: setInterval/click handlers are already
    // zone-patched, so zone.js schedules change detection for us. Forcing an
    // extra synchronous detectChanges() on top of that raced with other async
    // updates elsewhere in this view (e.g. a child component's fetch-based HTTP
    // response landing in the same tick) and caused NG0100.
  }

  prev() {
    const len = this.upcomingBookings.length || 1;
    this.currentIndex = (this.currentIndex - 1 + len) % len;
  }

  goToSlide(i: number) {
    this.currentIndex = i;
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
    } catch (err: unknown) {
      const errMsg = (err as any)?.error?.message || (err as any)?.message || 'Connection error. Please check your network and try again.';
      this.ngZone.run(() => {
        this.messages.push({ sender: 'bot', text: '⚠️ ' + errMsg, timestamp: new Date() });
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

  async clearChat() {
    const ok = await this.confirmDialog.confirm({
      title: 'Clear chat history?',
      message: 'This deletes your entire conversation with the AI assistant. This cannot be undone.',
      confirmLabel: 'Clear Chat',
      danger: true
    });
    if (!ok) return;

    this.messages = [];
    if (isPlatformBrowser(this.platformId)) localStorage.removeItem('chatHist');
    this.messages.push({
      sender: 'bot',
      text: '️ Chat cleared. Ready to help you plan your next adventure!',
      timestamp: new Date(),
    });
    this.cdr.detectChanges();
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

  // ─── Service catalog ─────────────────────────────────────────────
  loadCatalogServices(): void {
    this.catalogLoading = true;
    this.loadSubscriptions.add(this.serviceCatalogService.getActiveServices().subscribe({
      next: (page) => this.applyViewState(() => {
        this.catalogServices = page.content;
        this.catalogLoading = false;
      }),
      error: () => this.applyViewState(() => {
        this.catalogLoading = false;
      })
    }));
  }

  // ─── Travel packages ──────────────────────────────────────────────────
  loadPackages(): void {
    this.packagesLoading = true;
    this.loadSubscriptions.add(this.packageService.getPublishedPackages().subscribe({
      next: (page) => this.applyViewState(() => {
        this.packages = page.content;
        this.packagesLoading = false;
      }),
      error: () => this.applyViewState(() => {
        this.packagesLoading = false;
      })
    }));
  }

  packageSubtitle(pkg: TravelPackage): string {
    return `${pkg.currencyCode} ${pkg.totalPrice} · ${pkg.components.length} component${pkg.components.length === 1 ? '' : 's'}`;
  }

  openPackageDetails(pkg: TravelPackage): void {
    this.detailPackage = pkg;
  }

  closePackageDetails(): void {
    this.detailPackage = null;
  }

  bookPackageFromDetails(pkg: TravelPackage): void {
    this.detailPackage = null;
    this.openPackageBookingModal(pkg);
  }

  openPackageBookingModal(pkg: TravelPackage): void {
    this.packageBookingDraft = pkg;
    this.packageBookingDraftStartDate = this.minBookingDate;
    this.packageBookingDraftSubmitting = false;
    this.packageBookingDraftError = '';
  }

  closePackageBookingModal(): void {
    if (this.packageBookingDraftSubmitting) return;
    this.packageBookingDraft = null;
  }

  get canConfirmPackageBooking(): boolean {
    return !!this.packageBookingDraftStartDate
      && !this.packageBookingDraftSubmitting;
  }

  confirmPackageBooking(): void {
    const pkg = this.packageBookingDraft;
    if (!pkg || !pkg.packageId || this.packageBookingDraftSubmitting) return;

    this.packageBookingDraftSubmitting = true;
    this.packageBookingDraftError = '';
    this.cdr.detectChanges();

    const request: PackageBookRequest = {
      startDate: this.packageBookingDraftStartDate
    };

    this.packageService.bookPackage(pkg.packageId, request).subscribe({
      next: (booking) => {
        this.packageBookingDraftSubmitting = false;
        this.packageBookingDraft = null;
        this.payNowDraft = {
          bookingType: 'PACKAGE_BOOKING',
          bookingId: booking.packageBookingId,
          bookingReference: booking.packageBookingId,
          merchantName: booking.packageName,
          amount: booking.totalGrossAmount,
          currencyCode: pkg.currencyCode || 'BDT'
        };
        this.refreshBookingsAfterMutation();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.packageBookingDraftSubmitting = false;
        this.packageBookingDraftError = err?.error?.message || 'Failed to book this package. Please try again.';
        this.cdr.detectChanges();
      }
    });
  }

  // ─── Booking modal (delegated to shared ServiceBookingModal) ────────────
  get minBookingDate(): string {
    return new Date().toISOString().slice(0, 10);
  }

  openBookingModal(service: PublicServiceListing): void {
    this.bookingDraft = service;
  }

  closeBookingModal(): void {
    this.bookingDraft = null;
  }

  onServiceBooked(booking: VendorBooking): void {
    const service = this.bookingDraft;
    this.bookingDraft = null;
    this.payNowDraft = {
      bookingType: 'VENDOR_BOOKING',
      bookingId: booking.bookingId,
      bookingReference: booking.bookingId,
      merchantName: service?.vendorBusinessName || booking.vendorBusinessName || booking.serviceName,
      amount: booking.grossAmount,
      currencyCode: service?.currencyCode || 'BDT'
    };
    this.refreshBookingsAfterMutation();
    this.cdr.detectChanges();
  }

  closePayNowModal(): void {
    this.payNowDraft = null;
  }

  onEscapeKey(): void {
    if (this.payNowDraft) {
      this.closePayNowModal();
    } else if (this.packageBookingDraft) {
      this.closePackageBookingModal();
    } else if (this.detailService) {
      this.closeServiceDetails();
    } else if (this.detailPackage) {
      this.closePackageDetails();
    }
  }

  // ─── Service catalog card details ────────────────────────────────
  serviceSubtitle(service: PublicServiceListing): string {
    return `${service.vendorBusinessName} · ${service.currencyCode} ${service.basePrice.toFixed(2)} / ${service.pricingUnit}`;
  }

  servicePackageBadge(service: PublicServiceListing): string | null {
    return service.packageItems && service.packageItems.length > 0
      ? `📦 ${service.packageItems.length}-stop package`
      : null;
  }

  openServiceDetails(service: PublicServiceListing): void {
    this.detailService = service;
  }

  closeServiceDetails(): void {
    this.detailService = null;
  }

  bookFromDetails(service: PublicServiceListing): void {
    this.detailService = null;
    this.openBookingModal(service);
  }

  resolveImageUrl(url: string | null | undefined): string {
    if (!url) return '';
    if (/^https?:\/\//i.test(url)) return url;
    return `${environment.assetOrigin}${url}`;
  }

  // ─── Banners (admin-managed offers) ────────────────────────────
  loadBanners(): void {
    this.bannersLoading = true;
    this.loadSubscriptions.add(this.bannerService.getActiveBanners().subscribe({
      next: (banners) => this.applyViewState(() => {
        this.banners = banners || [];
        this.bannersLoading = false;
      }),
      error: () => this.applyViewState(() => {
        this.bannersLoading = false;
      })
    }));
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
    this.loadSubscriptions.add(this.vendorBookingService.getMyBookingStatusSummary().subscribe({
      next: (summary) => this.applyViewState(() => {
        this.bookingStatusSummary = summary;
        this.buildStats();
      }),
      error: () => {
        // Non-critical — dashboard still works without summary counts
      }
    }));
  }

  private loadAllBookingsForStats(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    this.loadSubscriptions.add(this.vendorBookingService.getMyBookings().subscribe({
      next: (bookings) => this.applyViewState(() => {
        this.allBookings = bookings;
        this.buildUpcomingBookings();
        this.buildStats();
      }),
      error: () => {
        // Non-critical — dashboard still works without stats/upcoming trips
      }
    }));
  }

  /**
   * Applies an HTTP-driven state mutation and forces a view update.
   *
   * This app's HttpClient uses withFetch(), and fetch() responses can resolve
   * outside Angular's zone (documented elsewhere in this file, in sendMessage()) —
   * zone.js then never schedules a tick, so without an explicit detectChanges()
   * here the fetched data silently never renders. Confirmed by removing this call
   * entirely: catalog services/destinations/banners kept showing their loading
   * state forever despite the HTTP request completing successfully.
   *
   * Note: this can still occasionally produce a harmless NG0100
   * (ExpressionChangedAfterItHasBeenCheckedError) console warning in dev mode if
   * two of these land close together — that check is stripped from production
   * builds entirely, so it never reaches real users. Correct rendering matters
   * more than a dev-console warning, so detectChanges() stays.
   */
  private applyViewState(update: () => void): void {
    update();
    this.cdr.detectChanges();
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

  bookingStatusLabel(status: string): string {
    return status.charAt(0) + status.slice(1).toLowerCase();
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

  private refreshBookingsAfterMutation(): void {
    this.loadBookingStatusSummary();
    this.loadAllBookingsForStats();
  }
}
