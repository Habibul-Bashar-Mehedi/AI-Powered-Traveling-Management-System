import { Component, OnInit, ChangeDetectorRef, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Card } from '../shared/card/card';
import { Modal } from '../shared/modal/modal';
import { ServiceBookingModal } from '../shared/service-booking-modal/service-booking-modal';
import { PayNowModal } from '../shared/pay-now-modal/pay-now-modal';
import { DestinationService } from '../services/destination.service';
import { Destination } from '../models/destination.model';
import { FoodService } from '../services/food.service';
import { Food } from '../models/food.model';
import { MarketService } from '../services/market.service';
import { Market } from '../models/market.model';
import { ItemService } from '../services/item.service';
import { TraditionalItem } from '../models/item.model';
import { ServiceCatalogService } from '../services/service-catalog.service';
import { PublicServiceListing, VendorBooking } from '../models/vendor.model';
import { PaymentBookingType } from '../models/payment.model';
import { environment } from '../../environments/environment';
import { LoadingState } from '../shared/loading-state/loading-state';

type FilterMode = 'none' | 'location' | 'destination';

@Component({
  selector: 'app-explore-nearby',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, Card, Modal, ServiceBookingModal, PayNowModal, LoadingState],
  templateUrl: './explore-nearby.html',
  styleUrls: ['./explore-nearby.css']
})
export class ExploreNearby implements OnInit {
  constructor(
    private cdr: ChangeDetectorRef,
    private destinationService: DestinationService,
    private foodService: FoodService,
    private marketService: MarketService,
    private itemService: ItemService,
    private serviceCatalogService: ServiceCatalogService,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  // ─── Filter controls ────────────────────────────────────────────────
  destinations: Destination[] = [];
  selectedDestinationId: number | null = null;
  radiusKm = 50;
  filterMode: FilterMode = 'none';
  locationError = '';
  locatingInProgress = false;
  private userLat: number | null = null;
  private userLng: number | null = null;

  // ─── Results ────────────────────────────────────────────────────────
  foods: Food[] = [];
  foodsLoading = false;
  markets: Market[] = [];
  marketsLoading = false;
  items: TraditionalItem[] = [];
  itemsLoading = false;

  // ─── Real bookable services (for food/items that are linked) ────────
  private serviceCatalog = new Map<string, PublicServiceListing>();

  // ─── Details modals (descriptive, no booking) ────────────────────────
  detailFood: Food | null = null;
  detailMarket: Market | null = null;
  detailItem: TraditionalItem | null = null;

  // ─── Booking modal (shared, real VendorService flow) ─────────────────
  bookingDraft: PublicServiceListing | null = null;
  payNowDraft: {
    bookingType: PaymentBookingType;
    bookingId: string;
    bookingReference: string;
    merchantName: string;
    amount: number;
    currencyCode: string;
  } | null = null;

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    this.destinationService.getAll().subscribe({
      next: (d) => { this.destinations = d; this.cdr.detectChanges(); },
      error: () => {}
    });
    this.loadServiceCatalog();
    this.loadAll();
  }

  private loadServiceCatalog(): void {
    this.serviceCatalogService.getActiveServices(0, 100).subscribe({
      next: (page) => {
        page.content.forEach(s => this.serviceCatalog.set(s.serviceId, s));
        this.cdr.detectChanges();
      },
      error: () => {}
    });
  }

  resolvedService(linkedServiceId?: string): PublicServiceListing | undefined {
    return linkedServiceId ? this.serviceCatalog.get(linkedServiceId) : undefined;
  }

  // ─── Filters ────────────────────────────────────────────────────────
  useMyLocation(): void {
    if (!isPlatformBrowser(this.platformId) || !navigator.geolocation) {
      this.locationError = 'Geolocation is not available in this browser.';
      return;
    }
    this.locatingInProgress = true;
    this.locationError = '';
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        this.userLat = pos.coords.latitude;
        this.userLng = pos.coords.longitude;
        this.filterMode = 'location';
        this.selectedDestinationId = null;
        this.locatingInProgress = false;
        this.loadAll();
        this.cdr.detectChanges();
      },
      (err) => {
        this.locatingInProgress = false;
        this.locationError = err.message || 'Could not get your location. Try selecting a destination instead.';
        this.cdr.detectChanges();
      },
      { timeout: 10000 }
    );
  }

  onDestinationSelected(): void {
    if (this.selectedDestinationId != null) {
      this.filterMode = 'destination';
      this.userLat = null;
      this.userLng = null;
      this.loadAll();
    }
  }

  clearFilter(): void {
    this.filterMode = 'none';
    this.selectedDestinationId = null;
    this.userLat = null;
    this.userLng = null;
    this.locationError = '';
    this.loadAll();
  }

  private nearbyQuery() {
    if (this.filterMode === 'location' && this.userLat != null && this.userLng != null) {
      return { lat: this.userLat, lng: this.userLng, radiusKm: this.radiusKm };
    }
    if (this.filterMode === 'destination' && this.selectedDestinationId != null) {
      return { destinationId: this.selectedDestinationId };
    }
    return {};
  }

  loadAll(): void {
    const query = this.nearbyQuery();

    this.foodsLoading = true;
    this.foodService.getNearby(query).subscribe({
      next: (f) => { this.foods = f; this.foodsLoading = false; this.cdr.detectChanges(); },
      error: () => { this.foodsLoading = false; this.cdr.detectChanges(); }
    });

    this.marketsLoading = true;
    this.marketService.getNearby(query).subscribe({
      next: (m) => { this.markets = m; this.marketsLoading = false; this.cdr.detectChanges(); },
      error: () => { this.marketsLoading = false; this.cdr.detectChanges(); }
    });

    this.itemsLoading = true;
    this.itemService.getNearby(query).subscribe({
      next: (i) => { this.items = i; this.itemsLoading = false; this.cdr.detectChanges(); },
      error: () => { this.itemsLoading = false; this.cdr.detectChanges(); }
    });
  }

  // ─── Food ────────────────────────────────────────────────────────────
  foodSubtitle(food: Food): string {
    const region = food.destination?.region ? ` · ${food.destination.region}` : '';
    return `${food.priceRange || 'Price varies'}${region}`;
  }

  openFoodDetails(food: Food): void {
    this.detailFood = food;
  }

  closeFoodDetails(): void {
    this.detailFood = null;
  }

  bookFood(food: Food): void {
    const service = this.resolvedService(food.linkedServiceId);
    if (!service) return;
    this.detailFood = null;
    this.bookingDraft = service;
  }

  // ─── Market ──────────────────────────────────────────────────────────
  marketSubtitle(market: Market): string {
    return market.operatingHours || market.operatingDays || market.location || '';
  }

  openMarketDetails(market: Market): void {
    this.detailMarket = market;
  }

  closeMarketDetails(): void {
    this.detailMarket = null;
  }

  // ─── Item ────────────────────────────────────────────────────────────
  itemSubtitle(item: TraditionalItem): string {
    return item.priceRange || (item.market?.name ? `At ${item.market.name}` : '');
  }

  openItemDetails(item: TraditionalItem): void {
    this.detailItem = item;
  }

  closeItemDetails(): void {
    this.detailItem = null;
  }

  orderItem(item: TraditionalItem): void {
    const service = this.resolvedService(item.linkedServiceId);
    if (!service) return;
    this.detailItem = null;
    this.bookingDraft = service;
  }

  // ─── Booking (shared component handles the actual reservation) ──────
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
    this.cdr.detectChanges();
  }

  closePayNowModal(): void {
    this.payNowDraft = null;
  }

  resolveImageUrl(url: string | null | undefined): string {
    if (!url) return '';
    if (/^https?:\/\//i.test(url)) return url;
    return `${environment.assetOrigin}${url}`;
  }
}
