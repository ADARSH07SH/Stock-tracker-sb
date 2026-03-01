const rateLimit = require('express-rate-limit');

const limiter = rateLimit({
  windowMs: 60000,
  max: 50,
  message: { status: 'error', message: 'Too many requests' },
  keyGenerator: () => 'global'
});

module.exports = limiter;
