const config = require('../config');
const { AppError } = require('../utils/errors');

const authenticate = (req, res, next) => {
  const apiKey = req.headers['x-api-key'] || req.headers.authorization?.replace('Bearer ', '');

  if (!apiKey) {
    throw new AppError('API key is required', 401);
  }

  if (apiKey !== config.apiKey) {
    throw new AppError('Invalid API key', 403);
  }

  next();
};

module.exports = authenticate;
