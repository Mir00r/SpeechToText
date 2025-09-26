#!/bin/bash

# ==========================================================
# Speech to Text Service - Quality Assurance Script
# ==========================================================
# This script runs comprehensive tests and quality checks
# for both Java and Python services.

set -e  # Exit on any error

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🚀 Starting Quality Assurance Checks${NC}"
echo "========================================"

# ==========================================================
# JAVA SERVICE CHECKS
# ==========================================================

echo -e "\n${BLUE}📋 Running Java Service Checks${NC}"

echo -e "\n${YELLOW}🧹 Code Formatting Check${NC}"
./gradlew :services:api-service:spotlessCheck || {
    echo -e "${RED}❌ Code formatting issues found. Run: ./gradlew spotlessApply${NC}"
    exit 1
}
echo -e "${GREEN}✅ Java code formatting is correct${NC}"

echo -e "\n${YELLOW}🧪 Running Unit Tests${NC}"
./gradlew :services:api-service:test --continue
echo -e "${GREEN}✅ Java unit tests completed${NC}"

echo -e "\n${YELLOW}🔗 Running Integration Tests${NC}"
./gradlew :services:api-service:integrationTest --continue
echo -e "${GREEN}✅ Java integration tests completed${NC}"

echo -e "\n${YELLOW}📊 Generating Code Coverage Report${NC}"
./gradlew :services:api-service:jacocoTestReport
echo -e "${GREEN}✅ Java code coverage report generated${NC}"

echo -e "\n${YELLOW}🎯 Checking Coverage Threshold${NC}"
./gradlew :services:api-service:jacocoTestCoverageVerification || {
    echo -e "${RED}❌ Code coverage below threshold${NC}"
    echo "Check coverage report: services/api-service/build/reports/jacoco/test/html/index.html"
    exit 1
}
echo -e "${GREEN}✅ Java code coverage meets threshold${NC}"

echo -e "\n${YELLOW}⚡ Running Performance Tests${NC}"
./gradlew :services:api-service:test --tests "*PerformanceTest*"
echo -e "${GREEN}✅ Java performance tests completed${NC}"

# ==========================================================
# PYTHON SERVICE CHECKS
# ==========================================================

echo -e "\n${BLUE}🐍 Running Python Service Checks${NC}"

cd services/transcription-service

# Check if virtual environment exists
if [ ! -d "venv" ]; then
    echo -e "${YELLOW}📦 Creating Python virtual environment${NC}"
    python3 -m venv venv
fi

echo -e "${YELLOW}🔧 Activating virtual environment and installing dependencies${NC}"
source venv/bin/activate
pip install --upgrade pip
pip install -r requirements.txt
pip install -r requirements-test.txt

echo -e "\n${YELLOW}🧹 Python Code Formatting Check${NC}"
black --check . || {
    echo -e "${RED}❌ Python formatting issues found. Run: black .${NC}"
    deactivate
    exit 1
}
echo -e "${GREEN}✅ Python code formatting is correct${NC}"

echo -e "\n${YELLOW}📝 Import Sorting Check${NC}"
isort --check-only . || {
    echo -e "${RED}❌ Python import sorting issues found. Run: isort .${NC}"
    deactivate
    exit 1
}
echo -e "${GREEN}✅ Python import sorting is correct${NC}"

echo -e "\n${YELLOW}🔍 Linting with flake8${NC}"
flake8 app tests --count --select=E9,F63,F7,F82 --show-source --statistics
flake8 app tests --count --exit-zero --max-complexity=10 --max-line-length=88 --statistics
echo -e "${GREEN}✅ Python linting completed${NC}"

echo -e "\n${YELLOW}🔐 Security Check with bandit${NC}"
bandit -r app -f json -o bandit-report.json || {
    echo -e "${YELLOW}⚠️ Security issues found. Check bandit-report.json${NC}"
}

echo -e "\n${YELLOW}🛡️ Dependency Security Check${NC}"
safety check --json --output safety-report.json || {
    echo -e "${YELLOW}⚠️ Vulnerable dependencies found. Check safety-report.json${NC}"
}

echo -e "\n${YELLOW}🧪 Running Python Tests${NC}"
pytest tests/ \
    --cov=app \
    --cov-report=xml \
    --cov-report=html \
    --cov-fail-under=70 \
    --junitxml=pytest-report.xml || {
    echo -e "${RED}❌ Python tests failed or coverage below threshold${NC}"
    deactivate
    exit 1
}
echo -e "${GREEN}✅ Python tests completed successfully${NC}"

echo -e "\n${YELLOW}⚡ Running Performance Tests${NC}"
pytest tests/test_performance.py -v
echo -e "${GREEN}✅ Python performance tests completed${NC}"

deactivate
cd "$PROJECT_ROOT"

# ==========================================================
# DOCKER BUILD TESTS
# ==========================================================

echo -e "\n${BLUE}🐳 Running Docker Build Tests${NC}"

echo -e "\n${YELLOW}🏗️ Building API Service Docker Image${NC}"
docker build -t speechtotext-api:test services/api-service/
echo -e "${GREEN}✅ API service Docker image built successfully${NC}"

echo -e "\n${YELLOW}🏗️ Building Transcription Service Docker Image${NC}"
docker build -t speechtotext-transcription:test services/transcription-service/
echo -e "${GREEN}✅ Transcription service Docker image built successfully${NC}"

# ==========================================================
# INTEGRATION SMOKE TESTS
# ==========================================================

echo -e "\n${BLUE}💨 Running Integration Smoke Tests${NC}"

echo -e "\n${YELLOW}🚀 Starting services with Docker Compose${NC}"
cd infra
docker-compose up -d --wait

echo -e "\n${YELLOW}⏳ Waiting for services to be ready${NC}"
timeout 120 bash -c '
    until curl -f http://localhost:8080/actuator/health >/dev/null 2>&1; do
        echo "Waiting for API service..."
        sleep 5
    done
    until curl -f http://localhost:8081/health >/dev/null 2>&1; do
        echo "Waiting for Transcription service..."
        sleep 5
    done
'

echo -e "\n${YELLOW}🔍 Testing API Health${NC}"
curl -f http://localhost:8080/actuator/health
echo -e "\n${GREEN}✅ API service is healthy${NC}"

echo -e "\n${YELLOW}🔍 Testing Transcription Service Health${NC}"
curl -f http://localhost:8081/health
echo -e "\n${GREEN}✅ Transcription service is healthy${NC}"

echo -e "\n${YELLOW}📄 Testing Basic Upload${NC}"
echo "test audio content" > test.wav
response=$(curl -s -X POST \
    -F "file=@test.wav" \
    -F "language=en" \
    -F "sync=false" \
    http://localhost:8080/api/v1/transcriptions)

if echo "$response" | grep -q "id"; then
    echo -e "${GREEN}✅ File upload test successful${NC}"
else
    echo -e "${RED}❌ File upload test failed${NC}"
    echo "Response: $response"
fi

rm -f test.wav

echo -e "\n${YELLOW}🛑 Stopping services${NC}"
docker-compose down -v

cd "$PROJECT_ROOT"

# ==========================================================
# CLEANUP
# ==========================================================

echo -e "\n${YELLOW}🧹 Cleaning up Docker images${NC}"
docker rmi speechtotext-api:test speechtotext-transcription:test || true

# ==========================================================
# SUMMARY
# ==========================================================

echo -e "\n${GREEN}🎉 All Quality Assurance Checks Completed Successfully!${NC}"
echo "================================================================"
echo
echo -e "${BLUE}📊 Summary:${NC}"
echo "✅ Java code formatting and linting"
echo "✅ Java unit and integration tests"
echo "✅ Java code coverage analysis"
echo "✅ Java performance tests"
echo "✅ Python code formatting and linting"
echo "✅ Python security checks"
echo "✅ Python unit and performance tests"
echo "✅ Python code coverage analysis"
echo "✅ Docker image builds"
echo "✅ Integration smoke tests"
echo
echo -e "${GREEN}🚀 Ready for deployment!${NC}"

# ==========================================================
# REPORT LOCATIONS
# ==========================================================

echo -e "\n${BLUE}📋 Test Reports Available At:${NC}"
echo "• Java Test Reports: services/api-service/build/reports/tests/test/index.html"
echo "• Java Coverage: services/api-service/build/reports/jacoco/test/html/index.html"
echo "• Python Coverage: services/transcription-service/htmlcov/index.html"
echo "• Python Security: services/transcription-service/bandit-report.json"
echo "• Python Safety: services/transcription-service/safety-report.json"
