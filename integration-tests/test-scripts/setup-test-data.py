#!/usr/bin/env python3
"""
Setup test data in MongoDB for WCFC Manuals integration tests
"""

import json
import os
import sys
import uuid

try:
    from pymongo import MongoClient
except ImportError:
    print("PyMongo not available. Please install it with: pip install pymongo")
    sys.exit(1)

def setup_mongodb_data():
    """Setup test data in MongoDB"""
    print("Setting up MongoDB test data...")
    
    # Connect to MongoDB
    client = MongoClient('mongodb://localhost:27017/')
    db = client['wcfc-quiz']

    # Clear existing test data
    db.Question.delete_many({})
    db.Recipe.delete_many({})
    db.Record.delete_many({})
    
    # Create test questions
    test_questions = [
        {
            "questionId": 1001,
            "question": "What is the standard traffic pattern altitude for most airports?",
            "answers": [
                {"answer": "800 feet AGL", "correct": False},
                {"answer": "1000 feet AGL", "correct": True},
                {"answer": "1200 feet AGL", "correct": False},
                {"answer": "1500 feet AGL", "correct": False}
            ],
            "discussion": "The standard traffic pattern altitude is 1000 feet above ground level (AGL) unless otherwise specified.",
            "references": "AIM 4-3-3",
            "attributes": ["STUDENT", "TRAFFIC_PATTERN"],
            "category": "REGULATIONS",
            "type": "MULTIPLE_CHOICE",
            "supersededBy": -1
        },
        {
            "questionId": 1002,
            "question": "What are the minimum weather conditions for VFR flight in Class E airspace below 10,000 feet MSL?",
            "answers": [
                {"answer": "1 mile visibility, clear of clouds", "correct": False},
                {"answer": "3 miles visibility, 500 feet below clouds, 1000 feet above clouds, 2000 feet horizontal", "correct": True},
                {"answer": "5 miles visibility, 1000 feet below clouds, 1000 feet above clouds, 1 mile horizontal", "correct": False},
                {"answer": "3 miles visibility, clear of clouds", "correct": False}
            ],
            "discussion": "In Class E airspace below 10,000 feet MSL, VFR minimums are 3 miles visibility and cloud clearances of 500 feet below, 1000 feet above, and 2000 feet horizontal.",
            "references": "14 CFR 91.155",
            "attributes": ["STUDENT", "WEATHER", "AIRSPACE"],
            "category": "REGULATIONS",
            "type": "MULTIPLE_CHOICE",
            "supersededBy": -1
        },
        {
            "questionId": 1003,
            "question": "What is the maximum demonstrated crosswind component for a Cessna 152?",
            "answers": [
                {"answer": "10 knots", "correct": False},
                {"answer": "12 knots", "correct": True},
                {"answer": "15 knots", "correct": False},
                {"answer": "20 knots", "correct": False}
            ],
            "discussion": "The Cessna 152 has a maximum demonstrated crosswind component of 12 knots.",
            "references": "C152 POH",
            "attributes": ["STUDENT", "C152", "PERFORMANCE"],
            "category": "AIRCRAFT",
            "type": "MULTIPLE_CHOICE",
            "supersededBy": -1
        }
    ]
    
    # Insert test questions
    db.Question.insert_many(test_questions)
    print(f"Created {len(test_questions)} test questions")
    
    # Create test recipe
    test_recipe = {
        "recipeId": 101,
        "name": "Student Test Quiz",
        "alias": "STUDENT_TEST",
        "order": 1,
        "questionCount": 3,
        "attributes": ["STUDENT"],
        "exclusions": []
    }
    
    # Insert test recipe
    db.Recipe.insert_one(test_recipe)
    print("Created test recipe: STUDENT_TEST")
    
    # Note: Admin user will be created automatically by the application
    # using the ADMIN_EMAIL, ADMIN_NAME, and ADMIN_PASSWORD configuration parameters
    
    print("MongoDB test data setup completed successfully!")
    
    client.close()

if __name__ == "__main__":
    try:
        setup_mongodb_data()
        print("Test data setup completed successfully!")
    except Exception as e:
        print(f"Error setting up test data: {e}")
        sys.exit(1)
