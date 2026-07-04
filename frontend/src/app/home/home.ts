import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { DestinationService } from '../services/destination.service';
import { Destination } from '../models/destination.model';

interface FeatureCard {
  icon: string;
  title: string;
  description: string;
}

interface HowItWorksStep {
  step: string;
  title: string;
  description: string;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './home.html',
  styleUrls: ['./home.css'],
})
export class Home implements OnInit {
  destinations: Destination[] = [];
  destinationsLoading = false;

  readonly features: FeatureCard[] = [
    {
      icon: 'chat',
      title: 'AI Travel Assistant',
      description: 'Get instant itinerary ideas, budget estimates, and destination recommendations from an assistant available around the clock.',
    },
    {
      icon: 'hotel',
      title: 'Hotel & Room Booking',
      description: 'Browse verified hotels and rooms, compare prices, and confirm your stay in just a few clicks.',
    },
    {
      icon: 'compass',
      title: 'Tours & Local Experiences',
      description: 'Discover guided tours, traditional food, and local markets curated by trusted vendors across every region.',
    },
    {
      icon: 'route',
      title: 'Transport Booking',
      description: 'Reserve routes and transport options between destinations without leaving the platform.',
    },
    {
      icon: 'shield',
      title: 'Verified Vendors',
      description: 'Every hotel, tour operator, and transport provider is reviewed and approved by our admin team before going live.',
    },
    {
      icon: 'track',
      title: 'Real-Time Booking Tracking',
      description: 'Track every request from pending to confirmed, with status updates and cancellation support in one dashboard.',
    },
  ];

  readonly steps: HowItWorksStep[] = [
    {
      step: '01',
      title: 'Create your free account',
      description: 'Sign up as a traveler in under a minute — or register your business as a vendor.',
    },
    {
      step: '02',
      title: 'Explore & book',
      description: 'Browse destinations, hotels, tours, and transport, or ask the AI assistant for a personalized plan.',
    },
    {
      step: '03',
      title: 'Manage everything in one place',
      description: 'Track bookings, chat with the AI assistant, and get support from your dashboard — anytime.',
    },
  ];

  constructor(
    private authService: AuthService,
    private destinationService: DestinationService,
  ) {}

  ngOnInit(): void {
    this.loadDestinations();
  }

  get isAuthenticated(): boolean {
    return this.authService.isAuthenticated();
  }

  get primaryCtaLink(): string {
    if (!this.isAuthenticated) return '/registration';
    const user = this.authService.getCurrentUserValue();
    return this.authService.getPostAuthRedirectUrl(user?.role);
  }

  get primaryCtaLabel(): string {
    return this.isAuthenticated ? 'Go to Dashboard' : 'Get Started Free';
  }

  private loadDestinations(): void {
    this.destinationsLoading = true;
    this.destinationService.getAll().subscribe({
      next: (destinations) => {
        this.destinations = destinations.slice(0, 6);
        this.destinationsLoading = false;
      },
      error: () => {
        this.destinationsLoading = false;
      },
    });
  }

  trackByFeature(index: number, feature: FeatureCard): string {
    return feature.title;
  }

  trackByStep(index: number, step: HowItWorksStep): string {
    return step.step;
  }

  trackByDestination(index: number, destination: Destination): number {
    return destination.id ?? index;
  }
}
