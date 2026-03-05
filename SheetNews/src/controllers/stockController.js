const googleSheetsService = require('../services/googleSheets');
const { AppError } = require('../utils/errors');

const getStockLinks = async (req, res, next) => {
  try {
    const { q } = req.query;
    // Limit to 15 for the sidebar display
    let links = await googleSheetsService.getStockScoreSheet(15);

    if (q) {
      const query = q.toLowerCase();
      links = links.filter(link =>
        (link.name && link.name.toLowerCase().includes(query)) ||
        (link.spreadsheetId && link.spreadsheetId.toLowerCase().includes(query))
      );
    }

    res.json({ status: 'success', data: links });
  } catch (error) {
    next(error);
  }
};

const searchStockLinks = async (req, res, next) => {
  try {
    const { q } = req.query;
    console.log('🔍 [searchStockLinks] Query parameter:', q);
    if (!q) throw new AppError('Query parameter "q" is required', 400);

    const results = await googleSheetsService.searchStockLinks(q);
    console.log('🔍 [searchStockLinks] Results count:', results.length);
    if (results.length > 0) {
      console.log('🔍 [searchStockLinks] First result:', results[0]);
    }
    res.json({ status: 'success', data: results });
  } catch (error) {
    console.error('❌ [searchStockLinks] Error:', error.message);
    next(error);
  }
};

const getStockNews = async (req, res, next) => {
  try {
    const { stock_name } = req.params;
    console.log('📰 [getStockNews] Requested stock name:', stock_name);
    console.log('📰 [getStockNews] Stock name length:', stock_name?.length);
    console.log('📰 [getStockNews] Stock name (encoded):', encodeURIComponent(stock_name));
    if (!stock_name) throw new AppError('Stock name required', 400);

    const result = await googleSheetsService.getStockNews(stock_name);
    console.log('📰 [getStockNews] Result:', result ? 'Success' : 'No data');
    if (result && result.data) {
      console.log('📰 [getStockNews] News items count:', result.data.length);
    }
    res.json({ status: 'success', data: result });
  } catch (error) {
    console.error('❌ [getStockNews] Error:', error.message);
    next(error);
  }
};

const getNewsBySpreadsheetId = async (req, res, next) => {
  try {
    const { id } = req.params;
    const { gid } = req.query;
    if (!id) throw new AppError('Spreadsheet ID required', 400);

    const result = await googleSheetsService.getNewsBySpreadsheetId(id, gid);
    res.json({ status: 'success', data: result });
  } catch (error) {
    next(error);
  }
};

const getMultipleStockNews = async (req, res, next) => {
  try {
    const { stocks } = req.body;
    if (!stocks || !Array.isArray(stocks) || stocks.length === 0) {
      throw new AppError('Stocks array required', 400);
    }

    const { results, errors } = await googleSheetsService.getMultipleStockNews(stocks);
    res.json({
      status: 'success',
      data: { results, errors: Object.keys(errors).length > 0 ? errors : undefined }
    });
  } catch (error) {
    next(error);
  }
};

module.exports = {
  getStockLinks,
  searchStockLinks,
  getStockNews,
  getMultipleStockNews,
  getNewsBySpreadsheetId
};
