# Stock News API

Simple API to fetch stock data from Google Sheets.

## Setup

1. Install:
```bash
npm install
```

2. Create `.env` file:
```env
PORT=3000
API_KEY=secret123
GOOGLE_SERVICE_ACCOUNT_EMAIL=your-email@project.iam.gserviceaccount.com
GOOGLE_PRIVATE_KEY="-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----\n"
GOOGLE_SPREADSHEET_ID=your-spreadsheet-id
```

3. Share your Google Spreadsheets with the service account email

4. Start:
```bash
npm start
```

5. Open: http://localhost:3000

## Usage

- Enter API Key: `secret123`
- Enter Stock Name: `361 W`
- Click "Fetch News"

## API Endpoints

**GET** `/api/sheet-news/:stock_name`
- Headers: `Authorization: Bearer secret123`

**POST** `/api/sheet-news`
- Headers: `Authorization: Bearer secret123`
- Body: `{ "stocks": ["361 W", "3M India"] }`

## Rate Limit

50 requests per minute globally.

## Testing

```bash
npm test
```
