#!/usr/bin/env bash
set -e

BASE_URL="${1:-http://localhost:8080}"
echo "=================================================="
echo " StudyWithUs — End-to-End Smoke Test"
echo " Target: ${BASE_URL}"
echo "=================================================="

# 1. Health check
echo -n "[1/7] Checking Actuator Health... "
HEALTH_RES=$(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/actuator/health")
if [ "$HEALTH_RES" != "200" ]; then
    echo "FAILED (HTTP ${HEALTH_RES})"
    exit 1
fi
echo "OK (200)"

# 2. Register user
TIMESTAMP=$(date +%s)
USERNAME="smoke_user_${TIMESTAMP}"
EMAIL="smoke_${TIMESTAMP}@example.com"
echo -n "[2/7] Registering new user (${USERNAME})... "
REGISTER_JSON=$(curl -s -X POST "${BASE_URL}/api/v1/auth/register" \
    -H "Content-Type: application/json" \
    -d "{
        \"username\": \"${USERNAME}\",
        \"name\": \"Smoke Test User\",
        \"email\": \"${EMAIL}\",
        \"password\": \"Password123!\",
        \"birthDate\": \"2000-01-01\",
        \"institution\": \"Universidade de SP\",
        \"course\": \"Engenharia\",
        \"bio\": \"Test account for deploy verification\"
    }")

ACCESS_TOKEN=$(echo "$REGISTER_JSON" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
if [ -z "$ACCESS_TOKEN" ]; then
    echo "FAILED"
    echo "Response: $REGISTER_JSON"
    exit 1
fi
echo "OK"

# 3. Get profile
echo -n "[3/7] Fetching user profile (/api/v1/users/me)... "
PROFILE_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/api/v1/users/me" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}")
if [ "$PROFILE_STATUS" != "200" ]; then
    echo "FAILED (HTTP ${PROFILE_STATUS})"
    exit 1
fi
echo "OK (200)"

# 4. Create community
echo -n "[4/7] Creating public community... "
COMMUNITY_JSON=$(curl -s -X POST "${BASE_URL}/api/v1/communities" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "{
        \"name\": \"Smoke Community ${TIMESTAMP}\",
        \"description\": \"Deploy verification community\",
        \"visibility\": \"PUBLIC\",
        \"rankingType\": \"WEEKLY\",
        \"timezone\": \"America/Sao_Paulo\"
    }")

COMMUNITY_ID=$(echo "$COMMUNITY_JSON" | grep -o '"id":"[^"]*' | head -n 1 | cut -d'"' -f4)
if [ -z "$COMMUNITY_ID" ]; then
    echo "FAILED"
    echo "Response: $COMMUNITY_JSON"
    exit 1
fi
echo "OK (ID: ${COMMUNITY_ID})"

# 5. Create check-in
echo -n "[5/7] Creating check-in (+10 pts)... "
CHECKIN_JSON=$(curl -s -X POST "${BASE_URL}/api/v1/checkins" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "{
        \"communityId\": \"${COMMUNITY_ID}\",
        \"caption\": \"Smoke test study checkin\",
        \"mediaKeys\": [\"notes_page_1.png\"]
    }")

CHECKIN_ID=$(echo "$CHECKIN_JSON" | grep -o '"id":"[^"]*' | head -n 1 | cut -d'"' -f4)
if [ -z "$CHECKIN_ID" ]; then
    echo "FAILED"
    echo "Response: $CHECKIN_JSON"
    exit 1
fi
echo "OK (ID: ${CHECKIN_ID})"

# 6. Check ranking
echo -n "[6/7] Checking community ranking... "
RANKING_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/api/v1/communities/${COMMUNITY_ID}/rankings/current" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}")
if [ "$RANKING_STATUS" != "200" ]; then
    echo "FAILED (HTTP ${RANKING_STATUS})"
    exit 1
fi
echo "OK (200)"

# 7. Check streak
echo -n "[7/7] Checking streak status... "
STREAK_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/api/v1/communities/${COMMUNITY_ID}/streaks/me" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}")
if [ "$STREAK_STATUS" != "200" ]; then
    echo "FAILED (HTTP ${STREAK_STATUS})"
    exit 1
fi
echo "OK (200)"

echo "=================================================="
echo " ALL SMOKE TESTS PASSED SUCCESSFULLY! "
echo " System is healthy, responsive, and production-ready."
echo "=================================================="
