#!/bin/bash

set -e

GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}Setting up configuration files${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""

STAGING_DIR="$HOME/configs-staging-example"
PROJECT_DIR="$HOME/Stock-tracker-sb"

if [ ! -d "$STAGING_DIR" ]; then
    echo -e "${RED}Error: Staging directory not found: $STAGING_DIR${NC}"
    echo -e "${YELLOW}Please upload the configs-staging-example folder to EC2 first.${NC}"
    echo ""
    echo -e "${BLUE}Expected structure:${NC}"
    echo "  ~/configs-staging-example/"
    echo "    ├── auth-application.yaml"
    echo "    ├── tracker-application.yaml"
    echo "    ├── ai-service.env"
    echo "    └── sheetnews.env"
    exit 1
fi

echo -e "${YELLOW}Checking for config files in staging...${NC}"
echo ""

MISSING_FILES=0

if [ ! -f "$STAGING_DIR/auth-application.yaml" ]; then
    echo -e "${RED}Missing: auth-application.yaml${NC}"
    MISSING_FILES=1
else
    echo -e "${GREEN}Found: auth-application.yaml${NC}"
fi

if [ ! -f "$STAGING_DIR/tracker-application.yaml" ]; then
    echo -e "${RED}Missing: tracker-application.yaml${NC}"
    MISSING_FILES=1
else
    echo -e "${GREEN}Found: tracker-application.yaml${NC}"
fi

if [ ! -f "$STAGING_DIR/ai-service.env" ]; then
    echo -e "${RED}Missing: ai-service.env${NC}"
    MISSING_FILES=1
else
    echo -e "${GREEN}Found: ai-service.env${NC}"
fi

if [ ! -f "$STAGING_DIR/sheetnews.env" ]; then
    echo -e "${RED}Missing: sheetnews.env${NC}"
    MISSING_FILES=1
else
    echo -e "${GREEN}Found: sheetnews.env${NC}"
fi

if [ $MISSING_FILES -eq 1 ]; then
    echo ""
    echo -e "${RED}Some config files are missing. Please add them to $STAGING_DIR${NC}"
    exit 1
fi

echo ""
echo -e "${YELLOW}Moving config files to project...${NC}"
echo ""

mkdir -p "$PROJECT_DIR/auth-service/src/main/resources"
mkdir -p "$PROJECT_DIR/tracker-service/src/main/resources"
mkdir -p "$PROJECT_DIR/ai-service"
mkdir -p "$PROJECT_DIR/SheetNews"

echo -e "${BLUE}Moving auth-service config...${NC}"
cp "$STAGING_DIR/auth-application.yaml" "$PROJECT_DIR/auth-service/src/main/resources/application.yaml"
chmod 600 "$PROJECT_DIR/auth-service/src/main/resources/application.yaml"
echo -e "${GREEN}Auth service config installed${NC}"
echo ""

echo -e "${BLUE}Moving tracker-service config...${NC}"
cp "$STAGING_DIR/tracker-application.yaml" "$PROJECT_DIR/tracker-service/src/main/resources/application.yaml"
chmod 600 "$PROJECT_DIR/tracker-service/src/main/resources/application.yaml"
echo -e "${GREEN}Tracker service config installed${NC}"
echo ""

echo -e "${BLUE}Moving AI service config...${NC}"
cp "$STAGING_DIR/ai-service.env" "$PROJECT_DIR/ai-service/.env"
chmod 600 "$PROJECT_DIR/ai-service/.env"
echo -e "${GREEN}AI service config installed${NC}"
echo ""

echo -e "${BLUE}Moving SheetNews config...${NC}"
cp "$STAGING_DIR/sheetnews.env" "$PROJECT_DIR/SheetNews/.env"
chmod 600 "$PROJECT_DIR/SheetNews/.env"
echo -e "${GREEN}SheetNews config installed${NC}"
echo ""

echo -e "${YELLOW}Removing staging directory...${NC}"
rm -rf "$STAGING_DIR"
echo -e "${GREEN}Staging directory removed${NC}"
echo ""

echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}All configs installed successfully${NC}"
echo -e "${GREEN}============================================${NC}"
echo ""
echo -e "${BLUE}Config files are now at:${NC}"
echo "  $PROJECT_DIR/auth-service/src/main/resources/application.yaml"
echo "  $PROJECT_DIR/tracker-service/src/main/resources/application.yaml"
echo "  $PROJECT_DIR/ai-service/.env"
echo "  $PROJECT_DIR/SheetNews/.env"
echo ""
echo -e "${YELLOW}All files secured with chmod 600${NC}"
echo ""
echo -e "${BLUE}Next steps:${NC}"
echo "  1. Run: cd ~/Stock-tracker-sb"
echo "  2. Run: docker compose down"
echo "  3. Run: docker compose up -d --build"
echo ""
