package com.financeflow.service;

import com.financeflow.entity.Transaction;
import com.financeflow.entity.User;
import com.financeflow.exception.CsvImportException;
import com.financeflow.repository.TransactionRepository;
import com.financeflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Exports the current user's transactions as CSV or Excel (XLSX).
 * The date range is optional; when both bounds are null the full history is exported.
 */
@Service
@RequiredArgsConstructor
public class TransactionExportService {

    private static final String[] HEADERS = {
            "Date", "Description", "Amount", "Type", "Category", "Account", "Reference"};
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public byte[] exportCsv(LocalDateTime start, LocalDateTime end) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (CSVPrinter printer = new CSVPrinter(
                new OutputStreamWriter(output, StandardCharsets.UTF_8),
                CSVFormat.DEFAULT.builder().setHeader(HEADERS).build())) {
            for (Transaction transaction : findTransactions(start, end)) {
                printer.printRecord(
                        transaction.getTransactionDate().format(DATE_FORMAT),
                        transaction.getDescription(),
                        transaction.getAmount(),
                        transaction.getTransactionType(),
                        transaction.getCategory() != null ? transaction.getCategory().getName() : "",
                        transaction.getAccount() != null ? transaction.getAccount().getAccountName() : "",
                        transaction.getReference() == null ? "" : transaction.getReference());
            }
            printer.flush();
        } catch (IOException ex) {
            throw new CsvImportException("Could not generate CSV export: " + ex.getMessage());
        }
        return output.toByteArray();
    }

    @Transactional(readOnly = true)
    public byte[] exportXlsx(LocalDateTime start, LocalDateTime end) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Transactions");
            writeHeader(sheet.createRow(0), workbook);

            int rowIndex = 1;
            for (Transaction transaction : findTransactions(start, end)) {
                writeRow(sheet.createRow(rowIndex++), transaction);
            }
            for (int column = 0; column < HEADERS.length; column++) {
                sheet.autoSizeColumn(column);
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new CsvImportException("Could not generate Excel export: " + ex.getMessage());
        }
    }

    private List<Transaction> findTransactions(LocalDateTime start, LocalDateTime end) {
        Long userId = getCurrentUserId();
        if (start != null && end != null) {
            return transactionRepository.findByUserIdBetweenOrderByTransactionDateDesc(userId, start, end);
        }
        if (start != null) {
            return transactionRepository.findByUserIdFromOrderByTransactionDateDesc(userId, start);
        }
        if (end != null) {
            return transactionRepository.findByUserIdUntilOrderByTransactionDateDesc(userId, end);
        }
        return transactionRepository.findByUserIdOrderByTransactionDateDesc(userId);
    }

    private void writeHeader(Row row, XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);

        for (int column = 0; column < HEADERS.length; column++) {
            Cell cell = row.createCell(column);
            cell.setCellValue(HEADERS[column]);
            cell.setCellStyle(style);
        }
    }

    private void writeRow(Row row, Transaction transaction) {
        row.createCell(0).setCellValue(transaction.getTransactionDate().format(DATE_FORMAT));
        row.createCell(1).setCellValue(safe(transaction.getDescription()));
        row.createCell(2).setCellValue(amount(transaction.getAmount()));
        row.createCell(3).setCellValue(transaction.getTransactionType().name());
        row.createCell(4).setCellValue(transaction.getCategory() != null
                ? safe(transaction.getCategory().getName()) : "");
        row.createCell(5).setCellValue(transaction.getAccount() != null
                ? safe(transaction.getAccount().getAccountName()) : "");
        row.createCell(6).setCellValue(safe(transaction.getReference()));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private double amount(BigDecimal value) {
        return value == null ? 0.0 : value.doubleValue();
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"))
                .getId();
    }
}
