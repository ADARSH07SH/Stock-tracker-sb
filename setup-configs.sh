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
    echo "    ├── sheetnews.env"
    echo "    ├── monitoring.conf"
    echo "    ├── prometheus.yml"
    echo "    ├── alertmanager.yml"
    echo "    ├── stock-tracker-alerts.yml"
    echo "    ├── prometheus-datasource.yml"
    echo "    ├── dashboard-config.yml"
    echo "    └── stock-tracker-overview.json"
    exit 1
fi

echo -e "${YELLOW}Checking for config files in staging...${NC}"
echo ""

MISSING_FILES=0

CONFIG_FILES=(
    "auth-application.yaml"
    "tracker-application.yaml"
    "ai-service.env"
    "sheetnews.env"
    "prometheus.yml"
    "alertmanager.yml"
    "stock-tracker-alerts.yml"
    "prometheus-datasource.yml"
    "dashboard-config.yml"
    "stock-tracker-overview.json"
)

OPTIONAL_FILES=(
    "monitoring.conf"
)

for file in "${CONFIG_FILES[@]}"; do
    if [ ! -f "$STAGING_DIR/$file" ]; then
        echo -e "${RED}Missing: $file${NC}"
        MISSING_FILES=1
    else
        echo -e "${GREEN}Found: $file${NC}"
    fi
done

for file in "${OPTIONAL_FILES[@]}"; do
    if [ ! -f "$STAGING_DIR/$file" ]; then
        echo -e "${YELLOW}Optional: $file not found${NC}"
    else
        echo -e "${GREEN}Found: $file${NC}"
    fi
done

if [ $MISSING_FILES -eq 1 ]; then
    echo ""
    echo -e "${RED}Some config files are missing. Please add them to $STAGING_DIR${NC}"
    exit 1
fi

echo ""
echo -e "${YELLOW}Creating directory structure...${NC}"
echo ""

mkdir -p "$PROJECT_DIR/auth-service/src/main/resources"
mkdir -p "$PROJECT_DIR/tracker-service/src/main/resources"
mkdir -p "$PROJECT_DIR/ai-service"
mkdir -p "$PROJECT_DIR/SheetNews"
mkdir -p "$PROJECT_DIR/monitoring-service/prometheus/rules"
mkdir -p "$PROJECT_DIR/monitoring-service/grafana/provisioning/datasources"
mkdir -p "$PROJECT_DIR/monitoring-service/grafana/provisioning/dashboards"
mkdir -p "$PROJECT_DIR/monitoring-service/grafana/dashboards"
mkdir -p "$PROJECT_DIR/monitoring-service/alertmanager"

echo -e "${BLUE}Moving application configs...${NC}"
cp "$STAGING_DIR/auth-application.yaml" "$PROJECT_DIR/auth-service/src/main/resources/application.yaml"
chmod 600 "$PROJECT_DIR/auth-service/src/main/resources/application.yaml"

cp "$STAGING_DIR/tracker-application.yaml" "$PROJECT_DIR/tracker-service/src/main/resources/application.yaml"
chmod 600 "$PROJECT_DIR/tracker-service/src/main/resources/application.yaml"

cp "$STAGING_DIR/ai-service.env" "$PROJECT_DIR/ai-service/.env"
chmod 600 "$PROJECT_DIR/ai-service/.env"

cp "$STAGING_DIR/sheetnews.env" "$PROJECT_DIR/SheetNews/.env"
chmod 600 "$PROJECT_DIR/SheetNews/.env"
echo -e "${GREEN}Application configs installed${NC}"
echo ""

echo -e "${BLUE}Moving monitoring configs...${NC}"
cp "$STAGING_DIR/prometheus.yml" "$PROJECT_DIR/monitoring-service/prometheus/prometheus.yml"
cp "$STAGING_DIR/alertmanager.yml" "$PROJECT_DIR/monitoring-service/alertmanager/alertmanager.yml"
cp "$STAGING_DIR/stock-tracker-alerts.yml" "$PROJECT_DIR/monitoring-service/prometheus/rules/stock-tracker-alerts.yml"
cp "$STAGING_DIR/prometheus-datasource.yml" "$PROJECT_DIR/monitoring-service/grafana/provisioning/datasources/prometheus.yml"
cp "$STAGING_DIR/dashboard-config.yml" "$PROJECT_DIR/monitoring-service/grafana/provisioning/dashboards/dashboard.yml"
cp "$STAGING_DIR/stock-tracker-overview.json" "$PROJECT_DIR/monitoring-service/grafana/dashboards/stock-tracker-overview.json"
echo -e "${GREEN}Monitoring configs installed${NC}"
echo ""

if [ -f "$STAGING_DIR/monitoring.conf" ]; then
    echo -e "${BLUE}Setting up Nginx monitoring config...${NC}"
    sudo cp "$STAGING_DIR/monitoring.conf" /etc/nginx/sites-available/monitoring.conf
    sudo ln -sf /etc/nginx/sites-available/monitoring.conf /etc/nginx/sites-enabled/monitoring.conf
    sudo nginx -t && sudo systemctl reload nginx
    echo -e "${GREEN}Nginx monitoring config installed${NC}"
    echo ""
fi

echo -e "${BLUE}Setting up monitoring permissions...${NC}"
sudo chown -R 472:472 "$PROJECT_DIR/monitoring-service/grafana/"
chmod 755 "$PROJECT_DIR/monitoring-service/"
chmod -R 644 "$PROJECT_DIR/monitoring-service/prometheus/"
chmod -R 644 "$PROJECT_DIR/monitoring-service/alertmanager/"
echo -e "${GREEN}Monitoring permissions set${NC}"
echo ""

if command -v ufw &> /dev/null; then
    echo -e "${BLUE}Configuring firewall...${NC}"
    sudo ufw allow 22/tcp
    sudo ufw allow 80/tcp
    sudo ufw allow 443/tcp
    sudo ufw deny 3000/tcp
    sudo ufw deny 9090/tcp
    sudo ufw deny 9093/tcp
    sudo ufw deny 9100/tcp
    echo -e "${GREEN}Firewall configured${NC}"
    echo ""
fi

echo -e "${YELLOW}Removing staging directory...${NC}"
rm -rf "$STAGING_DIR"
echo -e "${GREEN}Staging directory removed${NC}"
echo ""

echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}All configs installed successfully${NC}"
echo -e "${GREEN}============================================${NC}"
echo ""
echo -e "${BLUE}Application configs installed at:${NC}"
echo "  $PROJECT_DIR/auth-service/src/main/resources/application.yaml"
echo "  $PROJECT_DIR/tracker-service/src/main/resources/application.yaml"
echo "  $PROJECT_DIR/ai-service/.env"
echo "  $PROJECT_DIR/SheetNews/.env"
echo ""
echo -e "${BLUE}Monitoring configs installed at:${NC}"
echo "  $PROJECT_DIR/monitoring-service/prometheus/prometheus.yml"
echo "  $PROJECT_DIR/monitoring-service/alertmanager/alertmanager.yml"
echo "  $PROJECT_DIR/monitoring-service/prometheus/rules/stock-tracker-alerts.yml"
echo "  $PROJECT_DIR/monitoring-service/grafana/provisioning/"
echo "  $PROJECT_DIR/monitoring-service/grafana/dashboards/"
echo ""
echo -e "${YELLOW}All files secured with appropriate permissions${NC}"
echo ""
echo -e "${BLUE}Next steps:${NC}"
echo "  1. Run: cd ~/Stock-tracker-sb"
echo "  2. Run: docker-compose down"
echo "  3. Run: docker-compose up -d --build"
echo ""
echo -e "${YELLOW}Monitoring services will be available at:${NC}"
echo "  - Grafana: http://localhost:3000 (admin/admin123)"
echo "  - Prometheus: http://localhost:9090"
echo "  - AlertManager: http://localhost:9093"
echo ""
