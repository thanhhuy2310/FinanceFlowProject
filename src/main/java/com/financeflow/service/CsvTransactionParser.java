package com.financeflow.service;

import com.financeflow.enums.TransactionType;
import com.financeflow.exception.CsvImportException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Streaming, RFC-4180 compliant CSV parser for transaction imports.
 * <p>
 * The file must have the header: {@code date,description,amount,type,category,provider}.
 * Rows are parsed individually; a malformed row does not abort the import and is
 * reported as a row error instead. A structurally invalid file (empty, missing or
 * invalid header) aborts the import with a {@link CsvImportException}.
 */
@Component
public class CsvTransactionParser {

    private static final int EXPECTED_COLUMNS = 6;
    private static final List<String> EXPECTED_HEADERS =
            List.of("date", "description", "amount", "type", "category", "provider");

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public ParseResult parse(InputStream inputStream) {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .setIgnoreEmptyLines(true)
                .build();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            reader.mark(2);
            if (reader.read() == -1) {
                throw new CsvImportException("CSV file is empty");
            }
            reader.reset();

            try (CSVParser parser = format.parse(reader)) {
                validateHeaders(parser.getHeaderNames());

                List<CsvRow> rows = new ArrayList<>();
                List<CsvRowError> rowErrors = new ArrayList<>();

                for (CSVRecord record : parser) {
                    int rowNumber = (int) record.getRecordNumber() + 1;
                    try {
                        rows.add(parseRow(record, rowNumber));
                    } catch (CsvImportException ex) {
                        rowErrors.add(new CsvRowError(rowNumber, ex.getMessage()));
                    }
                }

                return new ParseResult(rows, rowErrors);
            }
        } catch (IOException ex) {
            throw new CsvImportException("Could not read CSV file: " + ex.getMessage());
        }
    }

    private void validateHeaders(List<String> headers) {
        if (headers == null || headers.isEmpty()) {
            throw new CsvImportException("CSV file is missing the header row");
        }

        List<String> normalized = new ArrayList<>(headers);
        if (normalized.get(0).startsWith("\uFEFF")) {
            normalized.set(0, normalized.get(0).substring(1));
        }
        normalized.replaceAll(header -> header.trim().toLowerCase(Locale.ROOT));

        if (!normalized.equals(EXPECTED_HEADERS)) {
            throw new CsvImportException(
                    "Invalid CSV header. Expected: date,description,amount,type,category,provider");
        }
    }

    private CsvRow parseRow(CSVRecord record, int rowNumber) {
        if (record.size() != EXPECTED_COLUMNS) {
            throw new CsvImportException("Expected " + EXPECTED_COLUMNS + " columns but found " + record.size());
        }

        String description = record.get(1).trim();
        if (description.isEmpty()) {
            throw new CsvImportException("Description is required");
        }

        String rawAmount = record.get(2).trim();
        BigDecimal amount = parseAmount(rawAmount);
        TransactionType transactionType = parseType(record.get(3).trim(), rawAmount);
        LocalDateTime transactionDate = parseDate(record.get(0).trim());

        return new CsvRow(
                rowNumber,
                transactionDate,
                description,
                amount,
                transactionType,
                blankToNull(record.get(4).trim()),
                blankToNull(record.get(5).trim()));
    }

    private BigDecimal parseAmount(String rawAmount) {
        if (rawAmount.isEmpty()) {
            throw new CsvImportException("Amount is required");
        }
        try {
            BigDecimal amount = new BigDecimal(rawAmount).abs();
            if (amount.signum() == 0) {
                throw new CsvImportException("Amount must not be zero");
            }
            return amount;
        } catch (NumberFormatException ex) {
            throw new CsvImportException("Invalid amount: " + rawAmount);
        }
    }

    private TransactionType parseType(String rawType, String rawAmount) {
        if (rawType.isEmpty()) {
            return rawAmount.startsWith("-") ? TransactionType.EXPENSE : TransactionType.INCOME;
        }

        String normalized = rawType.toUpperCase(Locale.ROOT);
        try {
            return TransactionType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new CsvImportException("Invalid transaction type: " + rawType);
        }
    }

    private LocalDateTime parseDate(String rawDate) {
        try {
            return LocalDate.parse(rawDate, DATE_FORMATTER).atStartOfDay();
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(rawDate, DATE_TIME_FORMATTER);
            } catch (DateTimeParseException ex) {
                throw new CsvImportException("Invalid date: " + rawDate + " (expected yyyy-MM-dd)");
            }
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public record CsvRow(
            int rowNumber,
            LocalDateTime transactionDate,
            String description,
            BigDecimal amount,
            TransactionType transactionType,
            String categoryName,
            String providerName) {
    }

    public record CsvRowError(int rowNumber, String errorMessage) {
    }

    public record ParseResult(List<CsvRow> rows, List<CsvRowError> rowErrors) {
    }
}
