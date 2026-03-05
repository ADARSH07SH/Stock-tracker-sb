const { google } = require('googleapis');
const config = require('../config');
const { AppError } = require('../utils/errors');

class GoogleSheetsService {
  constructor() {
    this.sheets = null;
    this.initialized = false;
    this.sheetNameCache = {};
  }

  async initialize() {
    if (this.initialized) return;

    try {
      if (!config.google.serviceAccountEmail || !config.google.privateKey) {
        throw new Error('Service account credentials are not configured');
      }

      const privateKey = config.google.privateKey.replace(/\\n/g, '\n');

      const auth = new google.auth.JWT(
        config.google.serviceAccountEmail,
        null,
        privateKey,
        ['https://www.googleapis.com/auth/spreadsheets.readonly']
      );

      await auth.authorize();
      this.sheets = google.sheets({ version: 'v4', auth });
      this.initialized = true;
    } catch (error) {
      throw new AppError('Failed to initialize Google Sheets API', 500);
    }
  }

  extractSpreadsheetId(url) {
    if (!url) return null;
    const match = url.match(/\/spreadsheets\/d\/(?:e\/)?([a-zA-Z0-9-_]+)/);
    return match ? match[1] : null;
  }

  extractGid(url) {
    if (!url) return null;
    const match = url.match(/[#&?]gid=([0-9]+)/);
    return match ? match[1] : null;
  }

  async getStockScoreSheet(limit = null) {
    await this.initialize();

    try {
      // If no limit, fetch all rows (up to 1000)
      const endRow = limit ? limit + 1 : 1000;

      const response = await this.sheets.spreadsheets.get({
        spreadsheetId: config.google.spreadsheetId,
        ranges: [`Stock_Score!A1:B${endRow}`],
        includeGridData: true
      });

      const sheet = response.data.sheets[0];
      const rows = sheet.data[0].rowData;

      if (!rows || rows.length === 0) {
        throw new AppError('No data found in Stock_Score sheet', 404);
      }

      const stockLinks = [];

      for (let i = 1; i < rows.length; i++) {

        if (limit && stockLinks.length >= limit) break;

        const row = rows[i];
        if (!row.values || !row.values[1]) continue;

        const cell = row.values[1];
        const hyperlink = cell.hyperlink;
        const displayValue = cell.formattedValue || cell.effectiveValue?.stringValue || '';

        if (hyperlink) {
          const spreadsheetId = this.extractSpreadsheetId(hyperlink);
          const gid = this.extractGid(hyperlink);
          if (spreadsheetId) {
            stockLinks.push({
              name: displayValue,
              url: hyperlink,
              spreadsheetId: spreadsheetId,
              gid: gid
            });
          }
        }
      }

      return stockLinks;
    } catch (error) {
      console.error('Error in getStockScoreSheet:', error);
      throw new AppError('Failed to fetch Stock_Score sheet', 500);
    }
  }

  async searchStockLinks(query) {
    await this.initialize();

    try {
      const response = await this.sheets.spreadsheets.get({
        spreadsheetId: config.google.spreadsheetId,
        ranges: ['Stock_Score!A:Z'],
        includeGridData: true
      });

      const sheet = response.data.sheets[0];
      const rows = sheet.data[0].rowData;

      if (!rows || rows.length === 0) return [];

      const headerRow = rows[0];
      const headers = headerRow.values ? headerRow.values.map(v => v.formattedValue || v.effectiveValue?.stringValue || '') : [];

      const results = [];
      const q = query.toLowerCase();

      for (let i = 1; i < rows.length; i++) {
        const row = rows[i];
        if (!row.values) continue;

        const info = {};
        let matches = false;

        headers.forEach((header, index) => {
          if (header) {
            const val = row.values[index];
            const displayVal = val ? (val.formattedValue || val.effectiveValue?.stringValue || '') : '';
            info[header] = displayVal;
            if (displayVal.toLowerCase().includes(q)) matches = true;
          }
        });

        if (matches) {
          const cell = row.values[1];
          results.push({
            name: cell?.formattedValue || cell?.effectiveValue?.stringValue || 'Unknown',
            url: cell?.hyperlink || null,
            spreadsheetId: cell?.hyperlink ? this.extractSpreadsheetId(cell.hyperlink) : null,
            gid: cell?.hyperlink ? this.extractGid(cell.hyperlink) : null
          });
        }
      }

      return results;
    } catch (error) {

      throw new AppError('Failed to search stock links', 500);
    }
  }

  async getSheetName(spreadsheetId, gid) {
    if (!gid) return 'A:Z';


    if (this.sheetNameCache[spreadsheetId] && this.sheetNameCache[spreadsheetId][gid]) {
      return `'${this.sheetNameCache[spreadsheetId][gid]}'!A:Z`;
    }

    try {

      const ss = await this.sheets.spreadsheets.get({ spreadsheetId });

      if (!this.sheetNameCache[spreadsheetId]) {
        this.sheetNameCache[spreadsheetId] = {};
      }

      ss.data.sheets.forEach(s => {
        this.sheetNameCache[spreadsheetId][s.properties.sheetId.toString()] = s.properties.title;
      });

      const title = this.sheetNameCache[spreadsheetId][gid];
      if (title) {
        return `'${title}'!A:Z`;
      }

      return 'A:Z';
    } catch (error) {

      return 'A:Z';
    }
  }

  async getSpreadsheetData(spreadsheetId, gid) {
    await this.initialize();

    try {
      const range = await this.getSheetName(spreadsheetId, gid);


      const response = await this.sheets.spreadsheets.values.get({
        spreadsheetId: spreadsheetId,
        range: range
      });

      const rows = response.data.values;

      if (!rows || rows.length === 0) {
        return [];
      }

      const headers = rows[0];
      const data = rows.slice(1).map(row => {
        const obj = {};
        headers.forEach((header, index) => {
          obj[header] = row[index] || '';
        });
        return obj;
      });


      return data;
    } catch (error) {
      throw new AppError(`Failed to fetch data from spreadsheet: ${spreadsheetId}`, 500);
    }
  }

  normalizeName(name) {
    if (!name) return '';
    return name.toLowerCase()
      .replace(/\+/g, ' ')
      .replace(/limited/g, '')
      .replace(/ltd/g, '')
      .replace(/industries/g, '')
      .replace(/industry/g, '')
      .replace(/enterprises/g, '')
      .replace(/corp/g, '')
      .replace(/[\s&.(),/-]+/g, '')
      .trim();
  }

  async getStockNews(stockName) {
    await this.initialize();

    try {
      console.log('📰 [getStockNews] Looking for stock:', stockName);
      // Load ALL stocks, not just first 15
      const stockLinks = await this.getStockScoreSheet();
      console.log('📰 [getStockNews] Total stocks in sheet:', stockLinks.length);
      
      const target = this.normalizeName(stockName);
      console.log('📰 [getStockNews] Normalized target:', target);

      // Log first few stock names for comparison
      console.log('📰 [getStockNews] First 5 stocks:', stockLinks.slice(0, 5).map(s => ({
        original: s.name,
        normalized: this.normalizeName(s.name)
      })));

      let stock = stockLinks.find(s => this.normalizeName(s.name) === target);
      console.log('📰 [getStockNews] Exact match found:', stock ? stock.name : 'No');

      if (!stock) {
        stock = stockLinks.find(s => {
          const current = this.normalizeName(s.name);
          return target.length > 2 && (current.includes(target) || target.includes(current));
        });
        console.log('📰 [getStockNews] Partial match found:', stock ? stock.name : 'No');
      }

      if (!stock) {
        console.error('❌ [getStockNews] Stock not found:', stockName);
        console.error('❌ [getStockNews] Available stocks:', stockLinks.map(s => s.name).join(', '));
        throw new AppError(`Stock not found: ${stockName}`, 404);
      }

      console.log('📰 [getStockNews] Found stock:', stock.name);
      console.log('📰 [getStockNews] Spreadsheet ID:', stock.spreadsheetId);
      console.log('📰 [getStockNews] GID:', stock.gid);

      const data = await this.getSpreadsheetData(stock.spreadsheetId, stock.gid);
      console.log('📰 [getStockNews] Data rows fetched:', data.length);

      return {
        stockName: stock.name,
        url: stock.url,
        data: data
      };
    } catch (error) {
      console.error('❌ [getStockNews] Error:', error.message);
      if (error instanceof AppError) {
        throw error;
      }
      throw new AppError('Failed to fetch stock news', 500);
    }
  }

  async getNewsBySpreadsheetId(spreadsheetId, gid) {
    await this.initialize();
    try {
      const data = await this.getSpreadsheetData(spreadsheetId, gid);

      return {
        spreadsheetId,
        gid,
        data: data
      };
    } catch (error) {
      if (error instanceof AppError) throw error;
      throw new AppError(`Failed to fetch news for ID: ${spreadsheetId}`, 500);
    }
  }

  async getMultipleStockNews(stockNames) {
    await this.initialize();

    const results = {};
    const errors = {};

    await Promise.all(
      stockNames.map(async (stockName) => {
        try {
          results[stockName] = await this.getStockNews(stockName);
        } catch (error) {
          errors[stockName] = error.message;
        }
      })
    );

    return { results, errors };
  }
}

module.exports = new GoogleSheetsService();
