#!/usr/bin/env python3
"""
Setup WireMock stubs for Gmail API mocking in WCFC Manuals integration tests
"""

import json
import requests
import sys
import time

def setup_wiremock_stubs():
    """Setup WireMock stubs"""
    
    wiremock_url = "http://localhost:8080"
    
    # Wait for WireMock to be ready
    for i in range(30):
        try:
            response = requests.get(f"{wiremock_url}/__admin/health")
            if response.status_code == 200:
                break
        except requests.exceptions.ConnectionError:
            pass
        time.sleep(1)
    else:
        raise Exception("WireMock is not ready")
    
    # Clear existing stubs
    requests.delete(f"{wiremock_url}/__admin/mappings")
    
    # Slack webhook stub - matches the pattern used in the application
    slack_stub = {
        "request": {
            "method": "POST",
            "urlPattern": "/services/slackapi.*"
        },
        "response": {
            "status": 200,
            "body": "ok"
        }
    }
    
    response = requests.post(f"{wiremock_url}/__admin/mappings", json=slack_stub)
    if response.status_code != 201:
        raise Exception(f"Failed to create Slack stub: {response.text}")
    
    print("Created Slack webhook stub")
    
    print("WireMock stubs setup completed successfully!")

if __name__ == "__main__":
    try:
        setup_wiremock_stubs()
        print("WireMock setup completed successfully!")
    except Exception as e:
        print(f"Error setting up WireMock: {e}")
        sys.exit(1)
