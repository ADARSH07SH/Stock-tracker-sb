package com.ash.tracker_service.config;

import com.ash.tracker_service.entity.StockHolding;
import com.ash.tracker_service.entity.UserPortfolio;
import com.ash.tracker_service.util.EncryptionUtil;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeSaveEvent;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PortfolioEncryptionListener extends AbstractMongoEventListener<UserPortfolio> {

    @Value("${app.encryption.key}")
    private String encryptionKey;

    @Override
    public void onBeforeSave(BeforeSaveEvent<UserPortfolio> event) {
        UserPortfolio portfolio = event.getSource();
        Document document = event.getDocument();
        if (portfolio.getStocks() == null || document == null) return;

        List<Document> stockDocs = document.getList("stocks", Document.class);
        if (stockDocs == null) return;

        for (int i = 0; i < stockDocs.size(); i++) {
            Document stockDoc = stockDocs.get(i);
            String stockName = stockDoc.getString("stockName");
            String isin = stockDoc.getString("isin");

            if (stockName != null && !EncryptionUtil.isEncrypted(stockName)) {
                stockDoc.put("stockName", EncryptionUtil.encrypt(stockName, encryptionKey));
            }
            if (isin != null && !EncryptionUtil.isEncrypted(isin)) {
                stockDoc.put("isin", EncryptionUtil.encrypt(isin, encryptionKey));
            }
        }
    }

    @Override
    public void onAfterConvert(AfterConvertEvent<UserPortfolio> event) {
        UserPortfolio portfolio = event.getSource();
        if (portfolio.getStocks() == null) return;

        for (StockHolding stock : portfolio.getStocks()) {
            if (EncryptionUtil.isEncrypted(stock.getStockName())) {
                stock.setStockName(EncryptionUtil.decrypt(stock.getStockName(), encryptionKey));
            }
            if (EncryptionUtil.isEncrypted(stock.getIsin())) {
                stock.setIsin(EncryptionUtil.decrypt(stock.getIsin(), encryptionKey));
            }
        }
    }
}
