#!/usr/bin/env python3
"""
Anvil-Bellows Model Manager
Dynamically add/remove/list models without restarting the proxy

Usage:
    python model_manager.py list                    # List all models
    python model_manager.py add <config.json>      # Add new model
    python model_manager.py remove <model_name>    # Remove model
    python model_manager.py info <model_name>      # Get model details
    python model_manager.py test <model_name>      # Test a model
"""

import argparse
import json
import os
import sys
import urllib.request
import urllib.error
from typing import Any, Dict, List, Optional


API_KEY = os.environ.get("LITELLM_MASTER_KEY", "sk-anvil-safe-key")
HOST = os.environ.get("LITELLM_HOST", "http://localhost:4000")

HEADERS = {
    "Authorization": f"Bearer {API_KEY}",
    "Content-Type": "application/json"
}


class ModelManager:
    def __init__(self, host: str = HOST, api_key: str = API_KEY):
        self.host = host.rstrip("/")
        self.api_key = api_key
        self.headers = {
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json"
        }

    def _request(self, method: str, endpoint: str, data: Optional[Dict] = None) -> Optional[Dict]:
        url = f"{self.host}{endpoint}"
        req = urllib.request.Request(url, data=json.dumps(data).encode() if data else None,
                                     headers=self.headers, method=method)
        try:
            with urllib.request.urlopen(req, timeout=10) as resp:
                return json.loads(resp.read().decode())
        except urllib.error.HTTPError as e:
            print(f"HTTP Error {e.code}: {e.reason}")
            try:
                error_body = json.loads(e.read().decode())
                print(json.dumps(error_body, indent=2))
            except:
                pass
            return None
        except Exception as e:
            print(f"Error: {e}")
            return None

    def list_models(self) -> bool:
        """List all configured models"""
        result = self._request("GET", "/model/list")
        if result:
            print(json.dumps(result, indent=2))
            return True
        return False

    def get_model_info(self, model_name: str) -> bool:
        """Get details of a specific model"""
        result = self._request("GET", f"/model/{model_name}/info")
        if result:
            print(json.dumps(result, indent=2))
            return True
        return False

    def add_model(self, config_path: str) -> bool:
        """Add a new model from config file"""
        try:
            with open(config_path, 'r') as f:
                config = json.load(f)
        except FileNotFoundError:
            print(f"Config file not found: {config_path}")
            return False
        except json.JSONDecodeError as e:
            print(f"Invalid JSON: {e}")
            return False

        result = self._request("POST", "/model/add", config)
        if result:
            print(f"Model added successfully: {config.get('model_name', 'unknown')}")
            return True
        return False

    def remove_model(self, model_name: str) -> bool:
        """Remove a model"""
        result = self._request("DELETE", f"/model/{model_name}")
        if result:
            print(f"Model removed: {model_name}")
            return True
        return False

    def test_model(self, model_name: str) -> bool:
        """Test a model with a simple prompt"""
        test_data = {
            "model": model_name,
            "messages": [{"role": "user", "content": "Say 'OK' if you can hear me."}],
            "max_tokens": 10
        }
        
        print(f"Testing model: {model_name}...")
        result = self._request("POST", "/v1/chat/completions", test_data)
        if result and "choices" in result:
            response = result["choices"][0]["message"]["content"]
            print(f"Response: {response}")
            return True
        print("Test failed")
        return False


def main():
    parser = argparse.ArgumentParser(description="Anvil-Bellows Model Manager")
    parser.add_argument("command", choices=["list", "add", "remove", "info", "test"],
                        help="Command to execute")
    parser.add_argument("argument", nargs="?", help="Model name or config path")
    parser.add_argument("--host", default=HOST, help="Proxy host")
    parser.add_argument("--api-key", default=API_KEY, help="API key")

    args = parser.parse_args()
    
    manager = ModelManager(host=args.host, api_key=args.api_key)
    
    if args.command == "list":
        manager.list_models()
    elif args.command == "add":
        if not args.argument:
            print("Error: config file required for add command")
            sys.exit(1)
        manager.add_model(args.argument)
    elif args.command == "remove":
        if not args.argument:
            print("Error: model name required for remove command")
            sys.exit(1)
        manager.remove_model(args.argument)
    elif args.command == "info":
        if not args.argument:
            print("Error: model name required for info command")
            sys.exit(1)
        manager.get_model_info(args.argument)
    elif args.command == "test":
        if not args.argument:
            print("Error: model name required for test command")
            sys.exit(1)
        manager.test_model(args.argument)


if __name__ == "__main__":
    main()