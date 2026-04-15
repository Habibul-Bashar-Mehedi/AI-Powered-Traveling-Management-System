import { Component, OnInit, OnDestroy, ChangeDetectorRef, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [FormsModule, HttpClientModule, CommonModule],
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.css'],
})
export class Dashboard implements OnInit, OnDestroy {
  constructor(
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  // ─── User Profile ───────────────────────────────────────────────
  user = {
    name: 'Aryan Rahman',
    role: 'Premium Traveler',
    avatar: 'https://i.pravatar.cc/80?img=12',
    joinedYear: 2022,
    tier: 'Gold',
    notifications: 3,
  };

  // ─── Stats ───────────────────────────────────────────────────────
  stats = [
    { label: 'Trips Booked', value: '24', icon: '✈️', trend: '+3 this month', color: 'cyan' },
    { label: 'Countries Visited', value: '11', icon: '🌍', trend: '+2 this year', color: 'violet' },
    { label: 'Total Spent', value: '৳2.4L', icon: '💳', trend: '↑ 12% vs last yr', color: 'amber' },
    { label: 'Reward Points', value: '8,420', icon: '⭐', trend: '340 pts expiring', color: 'rose' },
  ];

  // ─── Slides ──────────────────────────────────────────────────────
  slides = [
    {
      title: 'Welcome Back, Aryan 👋',
      subtitle: 'Your next adventure is just one booking away.',
      img: 'https://cdn-icons-png.flaticon.com/512/201/201623.png',
      accent: '#2563eb',
      tag: 'Dashboard',
    },
    {
      title: 'New Destinations 🌍',
      subtitle: 'Explore 40+ trending travel spots added this week.',
      img: 'https://cdn-icons-png.flaticon.com/512/854/854878.png',
      accent: '#0891b2',
      tag: 'Explore',
    },
    {
      title: 'Flash Sale — 30% Off 💰',
      subtitle: 'Limited time offers on international packages.',
      img: 'https://cdn-icons-png.flaticon.com/512/2910/2910791.png',
      accent: '#d97706',
      tag: 'Offers',
    },
    {
      title: 'Your Itinerary 🗓️',
      subtitle: 'Cox\'s Bazar trip starts in 5 days. All confirmed!',
      img: 'https://cdn-icons-png.flaticon.com/512/854/854878.png',
      accent: '#7c3aed',
      tag: 'Upcoming',
    },
  ];

  // ─── Destinations ─────────────────────────────────────────────────
  destinations = [
    {
      type: 'tour',
      name: "Cox's Bazar",
      location: 'Bangladesh',
      duration: '3 Days',
      group: '2–5 People',
      price: '12,000',
      rating: 4.8,
      reviews: 312,
      badge: 'Popular',
      img: 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=600&q=80',
    },
    {
      type: 'tour',
      name: 'Dubai Luxury',
      location: 'UAE',
      duration: '5 Days',
      group: '2–4 People',
      price: '50,000',
      rating: 4.9,
      reviews: 189,
      badge: 'Trending',
      img: 'https://images.unsplash.com/photo-1512453979798-5ea266f8880c?w=600&q=80',
    },
    {
      type: 'tour',
      name: 'Bangkok Adventure',
      location: 'Thailand',
      duration: '4 Days',
      group: '3–6 People',
      price: '30,000',
      rating: 4.7,
      reviews: 241,
      badge: 'Best Value',
      img: 'https://images.unsplash.com/photo-1506973035872-a4ec16b8e8d9?w=600&q=80',
    },
    {
      type: 'tour',
      name: 'Maldives Escape',
      location: 'Maldives',
      duration: '6 Days',
      group: '2 People',
      price: '85,000',
      rating: 5.0,
      reviews: 98,
      badge: 'Luxury',
      img: 'https://images.unsplash.com/photo-1514282401047-d79a71a590e8?w=600&q=80',
    },
    {
      type: 'special',
      name: 'Traditional Cuisine Tour',
      desc: 'Authentic local food journeys across 5 regions.',
      badge: 'Food',
      img: 'https://images.unsplash.com/photo-1604908176997-125f25cc6f3d?w=600&q=80',
    },
    {
      type: 'special',
      name: 'Heritage & Crafts',
      desc: 'Explore handmade textiles and traditional artisans.',
      badge: 'Culture',
      img: 'https://images.unsplash.com/photo-1603252109303-2751441dd157?w=600&q=80',
    },
  ];

  // ─── Active nav item ──────────────────────────────────────────────
  activeNav = 'dashboard';
  navItems = [
    { id: 'dashboard', label: 'Dashboard', icon: '⊞' },
    { id: 'trips', label: 'My Trips', icon: '✈️' },
    { id: 'explore', label: 'Explore', icon: '🧭' },
    { id: 'offers', label: 'Offers', icon: '🏷️' },
    { id: 'profile', label: 'Profile', icon: '👤' },
  ];

  // ─── Slider ───────────────────────────────────────────────────────
  currentIndex = 0;
  intervalId: any;

  ngOnInit() {
    this.startAutoSlide();
    this.loadChatHistory();
  }

  ngOnDestroy() {
    if (this.intervalId) clearInterval(this.intervalId);
  }

  startAutoSlide() {
    this.intervalId = setInterval(() => this.next(), 5000);
  }

  next() {
    this.currentIndex = (this.currentIndex + 1) % this.slides.length;
    this.cdr.detectChanges();
  }

  prev() {
    this.currentIndex = (this.currentIndex - 1 + this.slides.length) % this.slides.length;
    this.cdr.detectChanges();
  }

  goToSlide(i: number) {
    this.currentIndex = i;
    this.cdr.detectChanges();
  }

  // ─── Chatbot ──────────────────────────────────────────────────────
  showChatbot = false;
  userInput: string = '';
  isTyping = false;
  messages: any[] = [];
  chatMinimized = false;
  chatContext = `You are an expert AI travel assistant for a premium travel booking platform. 
  Help users plan trips, recommend destinations, suggest itineraries, estimate budgets, 
  and answer travel-related questions. Keep responses concise, friendly, and professional. 
  Use emojis sparingly for a warm tone. The user's name is Aryan Rahman.`;

  toggleChatbot() {
    this.showChatbot = !this.showChatbot;
    if (this.showChatbot && this.messages.length === 0) {
      this.messages.push({
        sender: 'bot',
        text: `Hello Aryan! 👋 I'm your AI Travel Assistant. I can help you plan trips, find deals, suggest destinations, or build custom itineraries. What would you like to explore today?`,
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
      const KEY = 'AIzaSyAgbDp6MNcHkP-lUmegSMpF-oRI7CeTLWQ';
      const url = `https://generativelanguage.googleapis.com/v1/models/gemini-1.5-flash:generateContent?key=${KEY}`;
      const conversationHistory = this.messages
        .slice(-8)
        .map((m: any) => ({ role: m.sender === 'user' ? 'user' : 'model', parts: [{ text: m.text }] }));

      const response = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          system_instruction: { parts: [{ text: this.chatContext }] },
          contents: conversationHistory,
        }),
      });
      const data = await response.json();
      const botText = data.candidates?.[0]?.content?.parts?.[0]?.text || 'Sorry, please try again.';
      this.messages.push({ sender: 'bot', text: botText, timestamp: new Date() });
      this.saveChatHistory();
    } catch {
      this.messages.push({ sender: 'bot', text: '⚠️ Connection error. Please check your network and try again.', timestamp: new Date() });
    } finally {
      this.isTyping = false;
      this.cdr.detectChanges();
      this.scrollToBottom();
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
        text: '🗑️ Chat cleared. Ready to help you plan your next adventure!',
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
      if (saved) this.messages = JSON.parse(saved);
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

  getStars(rating: number): string[] {
    return Array.from({ length: 5 }, (_, i) => (i < Math.floor(rating) ? '★' : '☆'));
  }
}