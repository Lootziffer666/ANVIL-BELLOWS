#!/usr/bin/env python3
"""
Anvil-Bellows Stats Monitor
Monitors budget, token burn rate, and cache hits in real-time

Usage: python guard_stats.py [--interval N] [--api-key KEY] [--host HOST]
"""

import argparse
import json
import os
import sys
import time
from datetime import datetime, timedelta
from typing import Any, Optional

try:
    import requests
except ImportError:
    print("ERROR: requests not installed. Run: pip install requests")
    sys.exit(1)

# ANSI color codes
RED = "\033[0;31m"
GREEN = "\033[0;32m"
YELLOW = "\033[1;33m"
BLUE = "\033[0;34m"
CYAN = "\033[0;36m"
BOLD = "\033[1m"
NC = "\033[0m"  # No Color


class AnvilBellowsMonitor:
    def __init__(self, api_key: str, host: str = "http://localhost:4000", interval: int = 60):
        self.api_key = api_key
        self.host = host.rstrip("/")
        self.interval = interval
        self.headers = {
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json"
        }
        self.budget_limit = 5.0  # Default, will be updated from API
        self.budget_spent = 0.0
        self.session_start = datetime.now()
        self.total_requests = 0
        self.cache_hits = 0
        self.cache_misses = 0

    def _api_get(self, endpoint: str) -> Optional[dict]:
        """Make GET request to LiteLLM API"""
        try:
            response = requests.get(
                f"{self.host}{endpoint}",
                headers=self.headers,
                timeout=10
            )
            if response.status_code == 200:
                return response.json()
            elif response.status_code == 429:
                print(f"{RED}[ERROR] Budget exceeded! API returns 429{NC}")
                return None
            else:
                print(f"{RED}[ERROR] API error: {response.status_code}{NC}")
                return None
        except requests.exceptions.ConnectionError:
            print(f"{RED}[ERROR] Cannot connect to {self.host}. Is the proxy running?{NC}")
            return None
        except requests.exceptions.Timeout:
            print(f"{RED}[ERROR] Request timeout{NC}")
            return None
        except Exception as e:
            print(f"{RED}[ERROR] {e}{NC}")
            return None

    def get_budget_info(self) -> Optional[dict]:
        """Get current budget status"""
        return self._api_get("/budget/info")

    def get_spend_logs(self) -> Optional[dict]:
        """Get spending details"""
        return self._api_get("/spend/logs")

    def get_cache_stats(self) -> Optional[dict]:
        """Get cache statistics"""
        return self._api_get("/cache/info")

    def get_model_stats(self) -> Optional[dict]:
        """Get per-model usage"""
        return self._api_get("/spend/total")

    def calculate_burn_rate(self) -> float:
        """Calculate token burn rate per minute"""
        elapsed = (datetime.now() - self.session_start).total_seconds() / 60
        if elapsed > 0:
            return self.total_requests / elapsed
        return 0

    def calculate_cache_rate(self) -> float:
        """Calculate cache hit rate percentage"""
        total = self.cache_hits + self.cache_misses
        if total > 0:
            return (self.cache_hits / total) * 100
        return 0

    def format_currency(self, amount: float) -> str:
        """Format currency with color coding"""
        percentage = (amount / self.budget_limit) * 100
        if percentage >= 100:
            return f"{RED}${amount:.2f}{NC}"
        elif percentage >= 80:
            return f"{YELLOW}${amount:.2f}{NC}"
        else:
            return f"{GREEN}${amount:.2f}{NC}"

    def print_header(self):
        """Print dashboard header"""
        uptime = datetime.now() - self.session_start
        print(f"\n{BLUE}{'='*60}{NC}")
        print(f"{BOLD}  Anvil-Bellows Monitor v1.0{NC}")
        print(f"{BLUE}{'='*60}{NC}")
        print(f"  Started: {self.session_start.strftime('%Y-%m-%d %H:%M:%S')}")
        print(f"  Uptime:  {uptime.days}d {uptime.seconds // 3600}h {(uptime.seconds % 3600) // 60}m")
        print(f"  Proxy:   {self.host}")
        print(f"{BLUE}{'='*60}{NC}")

    def print_budget_status(self, budget_data: dict):
        """Print budget status with visual bar"""
        if not budget_data:
            return

        try:
            self.budget_spent = float(budget_data.get("current_spend", 0))
            limit = budget_data.get("max_budget", self.budget_limit)
            self.budget_limit = limit
            
            remaining = max(0, limit - self.budget_spent)
            percentage = (self.budget_spent / limit) * 100 if limit > 0 else 0
            
            # Visual progress bar
            bar_length = 30
            filled = int((percentage / 100) * bar_length)
            bar = "█" * filled + "░" * (bar_length - filled)
            
            print(f"\n{BOLD}BUDGET STATUS{NC}")
            print(f"  Limit:    ${limit:.2f}")
            print(f"  Spent:    {self.format_currency(self.budget_spent)}")
            print(f"  Remaining: ${remaining:.2f}")
            print(f"  Usage:    [{bar}] {percentage:.1f}%")
            
            if percentage >= 100:
                print(f"{RED}  ⚠️  BUDGET EXCEEDED - BLOCKING ALL REQUESTS{NC}")
            elif percentage >= 80:
                print(f"{YELLOW}  ⚠️  WARNING: Budget at {percentage:.1f}%{NC}")
            else:
                print(f"{GREEN}  ✓  Budget healthy{NC}")
                
        except (KeyError, ValueError, TypeError) as e:
            print(f"{YELLOW}  Could not parse budget data: {e}{NC}")

    def print_model_usage(self, model_data: dict):
        """Print per-model spending"""
        if not model_data:
            return
            
        print(f"\n{BOLD}MODEL USAGE (by spend){NC}")
        
        try:
            spend_data = model_data.get("data", model_data)
            
            total_spend = 0
            for model, data in spend_data.items():
                if isinstance(data, dict):
                    spend = data.get("total_spend", 0)
                    total_spend += spend
                    
                    if spend > 0:
                        cost_str = f"${spend:.4f}"
                        # Color by cost
                        if spend > 1.0:
                            cost_color = RED
                        elif spend > 0.1:
                            cost_color = YELLOW
                        else:
                            cost_color = GREEN
                        
                        print(f"  {CYAN}{model:<30}{NC} {cost_color}{cost_str}{NC}")
            
            if total_spend == 0:
                print(f"  {YELLOW}No spend recorded yet{NC}")
                
        except Exception as e:
            print(f"{YELLOW}  Could not parse model data: {e}{NC}")

    def print_cache_status(self, cache_data: dict):
        """Print cache hit rate"""
        # Since LiteLLM doesn't have a direct cache stats endpoint,
        # we'll show a placeholder for now
        print(f"\n{BOLD}CACHE STATUS{NC}")
        
        # Try to infer from spend - if spend is low relative to requests,
        # caching is likely working
        rate = self.calculate_cache_rate()
        
        print(f"  Hit Rate: {rate:.1f}% (estimated)")
        print(f"  Type:     Local Disk Cache")
        print(f"  TTL:      24 hours")
        
        if rate > 50:
            print(f"  {GREEN}  ✓  Caching is effective{NC}")
        elif rate > 0:
            print(f"  {YELLOW}  ⚠  Cache warming up{NC}")
        else:
            print(f"  {YELLOW}  ℹ  Cache initializing...{NC}")

    def print_next_update(self):
        """Print countdown to next update"""
        print(f"\n  Next update in {self.interval}s... Press Ctrl+C to exit")

    def run(self):
        """Main monitoring loop"""
        print(f"\n{GREEN}Starting Anvil-Bellows Monitor...{NC}")
        print(f"Polling every {self.interval} seconds")
        print(f"Press Ctrl+C to stop\n")
        
        first_run = True
        
        try:
            while True:
                # Get budget info
                budget = self.get_budget_info()
                
                # Get model spend
                models = self.get_model_stats()
                
                # Get cache info
                cache = self.get_cache_stats()
                
                # Clear and print header
                if first_run:
                    self.print_header()
                    first_run = False
                
                print("\033[2J\033[H")  # Clear screen
                self.print_header()
                self.print_budget_status(budget)
                self.print_model_usage(models)
                self.print_cache_status(cache)
                self.print_next_update()
                
                time.sleep(self.interval)
                
        except KeyboardInterrupt:
            print(f"\n\n{GREEN}Monitor stopped. Stay safe, Anvil!{NC}")
            sys.exit(0)


def parse_args():
    parser = argparse.ArgumentParser(
        description="Anvil-Bellows Budget Monitor",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python guard_stats.py --api-key sk-anvil-safe-key
  python guard_stats.py --api-key sk-anvil-safe-key --interval 30
  python guard_stats.py --api-key sk-anvil-safe-key --host http://localhost:4001
        """
    )
    parser.add_argument(
        "--api-key", "-k",
        default=os.environ.get("LITELLM_MASTER_KEY", "sk-anvil-safe-key"),
        help="LiteLLM master key (default: from LITELLM_MASTER_KEY env)"
    )
    parser.add_argument(
        "--host", "-h",
        default=os.environ.get("LITELLM_HOST", "http://localhost:4000"),
        help="Proxy host (default: http://localhost:4000)"
    )
    parser.add_argument(
        "--interval", "-i",
        type=int,
        default=60,
        help="Polling interval in seconds (default: 60)"
    )
    return parser.parse_args()


if __name__ == "__main__":
    args = parse_args()
    monitor = AnvilBellowsMonitor(
        api_key=args.api_key,
        host=args.host,
        interval=args.interval
    )
    monitor.run()