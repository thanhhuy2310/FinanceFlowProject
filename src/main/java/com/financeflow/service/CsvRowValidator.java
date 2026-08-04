package com.financeflow.service;

import com.financeflow.enums.TransactionType;
import com.financeflow.exception.CsvImportException;
import com.financeflow.service.HeaderMapper.HeaderMapping;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * Validates and converts a single CSV data row into a {@link CsvTransactionParser.CsvRow}.
 * <p>
 * Kept separate from the parser so the parsing pipeline stays easy to follow:
 * {@code CsvTransactionParser} (streaming + header resolution) →
 * {@code HeaderMapper} (column mapping) → {@code CsvRowValidator} (per-row
 * validation) → {@code ImportBatchService} (persistence).
 */
@Component
public class CsvRowValidator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Validates the row and returns it as a {@link CsvTransactionParser.CsvRow}.
     * Throws {@link CsvImportException} with a human readable reason when any
     * field is missing or invalid.
     */
    public CsvTransactionParser.CsvRow parseRow(CSVRecord record, HeaderMapping header, int rowNumber) {
        if (record.size() != header.size()) {
            throw new CsvImportException("Expected " + header.size() + " columns but found " + record.size());
        }

        String description = value(record, header, CsvHeaderField.DESCRIPTION).trim();
        if (description.isEmpty()) {
            throw new CsvImportException("Description is required");
        }

        String rawAmount = value(record, header, CsvHeaderField.AMOUNT).trim();
        BigDecimal amount = parseAmount(rawAmount);
        TransactionType transactionType = parseType(
                value(record, header, CsvHeaderField.TRANSACTION_TYPE).trim(), rawAmount);
        LocalDateTime transactionDate = parseDate(value(record, header, CsvHeaderField.DATE).trim());

        return new CsvTransactionParser.CsvRow(
                rowNumber,
                transactionDate,
                description,
                amount,
                transactionType,
                blankToNull(value(record, header, CsvHeaderField.CATEGORY).trim()),
                blankToNull(value(record, header, CsvHeaderField.PROVIDER).trim()),
                blankToNull(value(record, header, CsvHeaderField.REFERENCE).trim()));
    }

    /**
     * Best-effort extraction of the description and category of a failing row,
     * so the import error report can show which row failed and with which data.
     * Never throws: the row is already known to be invalid.
     */
    public RowContext context(CSVRecord record, HeaderMapping header) {
        return new RowContext(
                blankToNull(value(record, header, CsvHeaderField.DESCRIPTION).trim()),
                blankToNull(value(record, header, CsvHeaderField.CATEGORY).trim()));
    }

    /** Reads a cell by its mapped column, returning an empty string when the column is absent. */
    private String value(CSVRecord record, HeaderMapping header, CsvHeaderField field) {
        int index = header.indexOf(field);
        return index < 0 ? "" : record.get(index);
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

    /** The raw values of a failing row, for the error report. */
    public record RowContext(String description, String categoryName) {
    }
}
