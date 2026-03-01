const express = require('express');
const router = express.Router();
const stockController = require('../controllers/stockController');
const authenticate = require('../middleware/auth');
const config = require('../config');
const { AppError } = require('../utils/errors');


router.get('/stock-links', authenticate, stockController.getStockLinks);


router.get('/stock-links/search', authenticate, stockController.searchStockLinks);


router.get('/sheet-news/:stock_name', authenticate, stockController.getStockNews);
router.get('/spreadsheet-news/:id', authenticate, stockController.getNewsBySpreadsheetId);
router.post('/sheet-news', authenticate, stockController.getMultipleStockNews);

module.exports = router;


