const promClient = require('prom-client');

// Create a Registry
const register = new promClient.Registry();

// Add default metrics
promClient.collectDefaultMetrics({
  register,
  prefix: 'sheetnews_'
});

// Custom metrics
const httpRequestsTotal = new promClient.Counter({
  name: 'sheetnews_http_requests_total',
  help: 'Total number of HTTP requests',
  labelNames: ['method', 'route', 'status_code'],
  registers: [register]
});

const httpRequestDuration = new promClient.Histogram({
  name: 'sheetnews_http_request_duration_seconds',
  help: 'Duration of HTTP requests in seconds',
  labelNames: ['method', 'route'],
  buckets: [0.1, 0.5, 1, 2, 5, 10],
  registers: [register]
});

const activeConnections = new promClient.Gauge({
  name: 'sheetnews_active_connections',
  help: 'Number of active connections',
  registers: [register]
});

const googleSheetsRequests = new promClient.Counter({
  name: 'sheetnews_google_sheets_requests_total',
  help: 'Total number of Google Sheets API requests',
  labelNames: ['status'],
  registers: [register]
});

const cacheHits = new promClient.Counter({
  name: 'sheetnews_cache_hits_total',
  help: 'Total number of cache hits',
  labelNames: ['type'],
  registers: [register]
});

// Middleware to collect metrics
const metricsMiddleware = (req, res, next) => {
  const start = Date.now();
  
  // Increment active connections
  activeConnections.inc();
  
  // Override res.end to capture metrics
  const originalEnd = res.end;
  res.end = function(...args) {
    const duration = (Date.now() - start) / 1000;
    
    // Record metrics
    httpRequestsTotal.inc({
      method: req.method,
      route: req.route?.path || req.path,
      status_code: res.statusCode
    });
    
    httpRequestDuration.observe({
      method: req.method,
      route: req.route?.path || req.path
    }, duration);
    
    // Decrement active connections
    activeConnections.dec();
    
    originalEnd.apply(this, args);
  };
  
  next();
};

// Helper functions
const recordGoogleSheetsRequest = (status) => {
  googleSheetsRequests.inc({ status });
};

const recordCacheHit = (type) => {
  cacheHits.inc({ type });
};

// Metrics endpoint
const getMetrics = async (req, res) => {
  try {
    res.set('Content-Type', register.contentType);
    const metrics = await register.metrics();
    res.end(metrics);
  } catch (error) {
    res.status(500).end(error.message);
  }
};

module.exports = {
  metricsMiddleware,
  recordGoogleSheetsRequest,
  recordCacheHit,
  getMetrics,
  register
};