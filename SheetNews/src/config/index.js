require('dotenv').config();

module.exports = {
  port: process.env.PORT || 3000,
  apiKey: process.env.API_KEY,
  google: {
    spreadsheetId: process.env.GOOGLE_SPREADSHEET_ID,
    serviceAccountEmail: process.env.GOOGLE_SERVICE_ACCOUNT_EMAIL,
    privateKey: process.env.GOOGLE_PRIVATE_KEY?.replace(/\\n/g, '\n')
  }
};
