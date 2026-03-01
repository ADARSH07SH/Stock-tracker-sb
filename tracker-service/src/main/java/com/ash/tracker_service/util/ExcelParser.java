package com.ash.tracker_service.util;

import com.ash.tracker_service.dto.ExcelStockRowDTO;
import org.apache.poi.ss.usermodel.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

public class ExcelParser {

    public static List<ExcelStockRowDTO> parse(MultipartFile file) {

        List<ExcelStockRowDTO> rows = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {

            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            int headerRowIndex = -1;

            for (int i = 0; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String firstCell = formatter.formatCellValue(row.getCell(0)).trim();
                if ("Stock Name".equalsIgnoreCase(firstCell)) {
                    headerRowIndex = i;
                    break;
                }
            }

            if (headerRowIndex == -1) {
                throw new RuntimeException("Stock table header not found");
            }

            for (int i = headerRowIndex + 1; i <= sheet.getLastRowNum(); i++) {

                Row row = sheet.getRow(i);
                if (row == null) continue;

                String stockName = formatter.formatCellValue(row.getCell(0)).trim();
                String isin = formatter.formatCellValue(row.getCell(1)).trim();

                if (stockName.isEmpty() || isin.isEmpty()) continue;

                int quantity = (int) parseDouble(formatter.formatCellValue(row.getCell(2)));
                double avgBuyPrice = parseDouble(formatter.formatCellValue(row.getCell(3)));

                ExcelStockRowDTO dto = new ExcelStockRowDTO();
                dto.setStockName(stockName);
                dto.setIsin(isin);
                dto.setQuantity(quantity);
                dto.setAverageBuyPrice(avgBuyPrice);

                rows.add(dto);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse excel", e);
        }

        return rows;
    }

    private static double parseDouble(String value) {
        if (value == null || value.trim().isEmpty()) return 0.0;
        return Double.parseDouble(value.replace(",", "").trim());
    }
}
