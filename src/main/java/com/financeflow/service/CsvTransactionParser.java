package com.financeflow.service;

import com.financeflow.enums.TransactionType;
import com.financeflow.exception.CsvImportException;
import com.financeflow.service.HeaderMapper.HeaderMapping;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Streaming, RFC-4180 compliant CSV parser for transaction imports.
 * <p>
 * The pipeline is: {@link HeaderMapper} resolves the header columns once per
 * file (aliases, case and whitespace tolerant, unknown columns ignored), then
 * {@link CsvRowValidator} parses and validates every data row. A malformed row
 * does not abort the import and is reported as a row error carrying the raw
 * description and category for a readable error report. A structurally invalid
 * file (empty, missing or invalid header) aborts the import with a
 * {@link CsvImportException} before any row is processed.
 */
@Component
public class CsvTransactionParser {

    private final HeaderMapper headerMapper = new HeaderMapper();
    private final CsvRowValidator rowValidator = new CsvRowValidator();

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
                // Resolve the header once; validation of mandatory columns happens
                // here, before any data row is read.
                HeaderMapping header = headerMapper.map(parser.getHeaderNames());

                List<CsvRow> rows = new ArrayList<>();
                List<CsvRowError> rowErrors = new ArrayList<>();

                for (CSVRecord record : parser) {
                    int rowNumber = (int) record.getRecordNumber() + 1;
                    try {
                        rows.add(rowValidator.parseRow(record, header, rowNumber));
                    } catch (CsvImportException ex) {
                        CsvRowValidator.RowContext context = rowValidator.context(record, header);
                        rowErrors.add(new CsvRowError(
                                rowNumber, ex.getMessage(), context.description(), context.categoryName()));
                    }
                }

                return new ParseResult(rows, rowErrors);
            }
        } catch (IOException ex) {
            throw new CsvImportException("Could not read CSV file: " + ex.getMessage());
        }
    }

    public record CsvRow(
            int rowNumber,
            LocalDateTime transactionDate,
            String description,
            BigDecimal amount,
            TransactionType transactionType,
            String categoryName,
            String providerName,
            String reference) {
    }

    /**
     * A failed data row. {@code description} and {@code categoryName} carry the
     * raw values found in the CSV so the UI can render a readable error report.
     */
    public record CsvRowError(
            int rowNumber,
            String errorMessage,
            String description,
            String categoryName) {

        public CsvRowError(int rowNumber, String errorMessage) {
            this(rowNumber, errorMessage, null, null);
        }
    }

    public record ParseResult(List<CsvRow> rows, List<CsvRowError> rowErrors) {
    }
}
