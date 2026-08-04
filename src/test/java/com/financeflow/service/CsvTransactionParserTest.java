package com.financeflow.service;

import com.financeflow.enums.TransactionType;
import com.financeflow.exception.CsvImportException;
import com.financeflow.service.CsvTransactionParser.CsvRow;
import com.financeflow.service.CsvTransactionParser.CsvRowError;
import com.financeflow.service.CsvTransactionParser.ParseResult;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvTransactionParserTest {

    private final CsvTransactionParser parser = new CsvTransactionParser();

    private ParseResult parse(String csv) {
        return parser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void parse_validRows_parsesAllRows() {
        String csv = """
                date,description,amount,type,category,provider
                2026-08-01,Coffee,-65000,EXPENSE,Food,Highlands
                2026-08-02,Salary,15000000,INCOME,Salary,Techcombank
                2026-08-03,Grab,50000,EXPENSE,Transport,MoMo
                """;

        ParseResult result = parse(csv);

        assertThat(result.rows()).hasSize(3);
        assertThat(result.rowErrors()).isEmpty();

        CsvRow coffee = result.rows().get(0);
        assertThat(coffee.transactionDate()).isEqualTo(LocalDateTime.parse("2026-08-01T00:00"));
        assertThat(coffee.description()).isEqualTo("Coffee");
        assertThat(coffee.amount()).isEqualByComparingTo("65000");
        assertThat(coffee.transactionType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(coffee.categoryName()).isEqualTo("Food");
        assertThat(coffee.providerName()).isEqualTo("Highlands");
    }

    @Test
    void parse_missingType_derivesFromAmountSign() {
        String csv = """
                date,description,amount,type,category,provider
                2026-08-01,Coffee,-65000,,Food,Highlands
                2026-08-02,Refund,120000,,Income,MoMo
                """;

        ParseResult result = parse(csv);

        assertThat(result.rows().get(0).transactionType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(result.rows().get(1).transactionType()).isEqualTo(TransactionType.INCOME);
    }

    @Test
    void parse_negativeExpenseAmount_normalizedToAbsoluteValue() {
        String csv = """
                date,description,amount,type,category,provider
                2026-08-01,Coffee,-65000,EXPENSE,Food,Highlands
                """;

        ParseResult result = parse(csv);

        assertThat(result.rows().get(0).amount()).isEqualByComparingTo("65000");
    }

    @Test
    void parse_quotedDescriptionWithComma_handled() {
        String csv = """
                date,description,amount,type,category,provider
                2026-08-01,"Coffee, Cafe Latte",50000,EXPENSE,Food,Highlands
                """;

        ParseResult result = parse(csv);

        assertThat(result.rows().get(0).description()).isEqualTo("Coffee, Cafe Latte");
    }

    @Test
    void parse_utf8BomHeader_accepted() {
        String csv = "\uFEFFdate,description,amount,type,category,provider\n"
                + "2026-08-01,Coffee,50000,EXPENSE,Food,Highlands\n";

        ParseResult result = parse(csv);

        assertThat(result.rows()).hasSize(1);
    }

    @Test
    void parse_caseInsensitiveHeaderAndType_accepted() {
        String csv = """
                Date,Description,Amount,Type,Category,Provider
                2026-08-01,Coffee,50000,expense,Food,Highlands
                """;

        ParseResult result = parse(csv);

        assertThat(result.rows()).hasSize(1);
        assertThat(result.rows().get(0).transactionType()).isEqualTo(TransactionType.EXPENSE);
    }

    @Test
    void parse_invalidHeader_throws() {
        String csv = """
                date,description,amount,type,category
                2026-08-01,Coffee,50000,EXPENSE,Food
                """;

        assertThatThrownBy(() -> parse(csv))
                .isInstanceOf(CsvImportException.class)
                .hasMessageContaining("Invalid CSV header");
    }

    @Test
    void parse_emptyFile_throws() {
        assertThatThrownBy(() -> parse(""))
                .isInstanceOf(CsvImportException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void parse_missingHeaderRow_throws() {
        String csv = """
                2026-08-01,Coffee,50000,EXPENSE,Food,Highlands
                """;

        assertThatThrownBy(() -> parse(csv))
                .isInstanceOf(CsvImportException.class)
                .hasMessageContaining("Invalid CSV header");
    }

    @Test
    void parse_wrongColumnCount_reportsRowError() {
        String csv = """
                date,description,amount,type,category,provider
                2026-08-01,Coffee,50000,EXPENSE,Food
                """;

        ParseResult result = parse(csv);

        assertThat(result.rows()).isEmpty();
        assertThat(result.rowErrors()).hasSize(1);
        assertThat(result.rowErrors().get(0).rowNumber()).isEqualTo(2);
        assertThat(result.rowErrors().get(0).errorMessage()).contains("Expected 6 columns");
    }

    @Test
    void parse_invalidDate_reportsRowError() {
        String csv = """
                date,description,amount,type,category,provider
                30/02/2026,Coffee,50000,EXPENSE,Food,Highlands
                """;

        ParseResult result = parse(csv);

        assertThat(result.rows()).isEmpty();
        assertThat(result.rowErrors()).hasSize(1);
        assertThat(result.rowErrors().get(0).errorMessage()).contains("Invalid date");
    }

    @Test
    void parse_datetimeValue_accepted() {
        String csv = """
                date,description,amount,type,category,provider
                2026-08-01T12:30:00,Coffee,50000,EXPENSE,Food,Highlands
                """;

        ParseResult result = parse(csv);

        assertThat(result.rows().get(0).transactionDate()).isEqualTo(LocalDateTime.parse("2026-08-01T12:30"));
    }

    @Test
    void parse_invalidAmount_reportsRowError() {
        String csv = """
                date,description,amount,type,category,provider
                2026-08-01,Coffee,abc,EXPENSE,Food,Highlands
                """;

        ParseResult result = parse(csv);

        assertThat(result.rowErrors()).hasSize(1);
        assertThat(result.rowErrors().get(0).errorMessage()).contains("Invalid amount");
    }

    @Test
    void parse_zeroAmount_reportsRowError() {
        String csv = """
                date,description,amount,type,category,provider
                2026-08-01,Coffee,0,EXPENSE,Food,Highlands
                """;

        ParseResult result = parse(csv);

        assertThat(result.rowErrors()).hasSize(1);
        assertThat(result.rowErrors().get(0).errorMessage()).contains("must not be zero");
    }

    @Test
    void parse_invalidType_reportsRowError() {
        String csv = """
                date,description,amount,type,category,provider
                2026-08-01,Coffee,50000,SAVINGS,Food,Highlands
                """;

        ParseResult result = parse(csv);

        assertThat(result.rowErrors()).hasSize(1);
        assertThat(result.rowErrors().get(0).errorMessage()).contains("Invalid transaction type");
    }

    @Test
    void parse_blankDescription_reportsRowError() {
        String csv = """
                date,description,amount,type,category,provider
                2026-08-01,,50000,EXPENSE,Food,Highlands
                """;

        ParseResult result = parse(csv);

        assertThat(result.rowErrors()).hasSize(1);
        assertThat(result.rowErrors().get(0).errorMessage()).contains("Description is required");
    }

    @Test
    void parse_mixedValidAndInvalidRows_keepsValidRows() {
        String csv = """
                date,description,amount,type,category,provider
                2026-08-01,Coffee,50000,EXPENSE,Food,Highlands
                30/02/2026,Bad date,50000,EXPENSE,Food,Highlands
                2026-08-03,Grab,30000,EXPENSE,Transport,MoMo
                """;

        ParseResult result = parse(csv);

        assertThat(result.rows()).hasSize(2);
        assertThat(result.rowErrors()).hasSize(1);
        assertThat(result.rowErrors().get(0).rowNumber()).isEqualTo(3);
    }

    @Test
    void parse_blankCategoryAndProvider_reportedAsNull() {
        String csv = """
                date,description,amount,type,category,provider
                2026-08-01,Coffee,50000,EXPENSE,,
                """;

        ParseResult result = parse(csv);

        assertThat(result.rows()).hasSize(1);
        assertThat(result.rows().get(0).categoryName()).isNull();
        assertThat(result.rows().get(0).providerName()).isNull();
    }

    @Test
    void parse_aliasHeadersIncludingVietnamese_supported() {
        String csv = """
                Ngày,Nội dung,Số tiền,Danh mục,Ngân hàng
                2026-08-01,Cà phê,-65000,Đồ ăn,Techcombank
                """;

        ParseResult result = parse(csv);

        assertThat(result.rows()).hasSize(1);
        assertThat(result.rowErrors()).isEmpty();

        CsvRow row = result.rows().get(0);
        assertThat(row.transactionDate()).isEqualTo(LocalDateTime.parse("2026-08-01T00:00"));
        assertThat(row.description()).isEqualTo("Cà phê");
        assertThat(row.amount()).isEqualByComparingTo("65000");
        assertThat(row.transactionType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(row.categoryName()).isEqualTo("Đồ ăn");
        assertThat(row.providerName()).isEqualTo("Techcombank");
    }

    @Test
    void parse_headersWithSurroundingSpaces_supported() {
        String csv = """
                 Date , Description , Amount , Type , Category , Provider
                2026-08-01,Coffee,50000,EXPENSE,Food,Highlands
                """;

        ParseResult result = parse(csv);

        assertThat(result.rows()).hasSize(1);
        assertThat(result.rows().get(0).description()).isEqualTo("Coffee");
    }

    @Test
    void parse_unknownColumns_areIgnored() {
        String csv = """
                date,description,amount,type,category,provider,location,phone,customer
                2026-08-01,Coffee,50000,EXPENSE,Food,Highlands,Hanoi,0123,John
                """;

        ParseResult result = parse(csv);

        assertThat(result.rows()).hasSize(1);
        assertThat(result.rowErrors()).isEmpty();
    }

    @Test
    void parse_referenceColumn_mapped() {
        String csv = """
                date,description,amount,type,category,provider,reference
                2026-08-01,Coffee,50000,EXPENSE,Food,Highlands,REF-2026-001
                """;

        ParseResult result = parse(csv);

        assertThat(result.rows()).hasSize(1);
        assertThat(result.rows().get(0).reference()).isEqualTo("REF-2026-001");
    }

    @Test
    void parse_typeColumnAbsent_derivesTypeFromAmountSign() {
        String csv = """
                date,description,amount,category,provider
                2026-08-01,Coffee,-65000,Food,Highlands
                2026-08-02,Salary,15000000,Salary,Techcombank
                """;

        ParseResult result = parse(csv);

        assertThat(result.rows()).hasSize(2);
        assertThat(result.rows().get(0).transactionType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(result.rows().get(1).transactionType()).isEqualTo(TransactionType.INCOME);
    }

    @Test
    void parse_missingRequiredColumn_throwsWithSupportedNames() {
        String csv = """
                date,description,type,category,provider
                2026-08-01,Coffee,50000,EXPENSE,Food
                """;

        assertThatThrownBy(() -> parse(csv))
                .isInstanceOf(CsvImportException.class)
                .hasMessageContaining("Invalid CSV header")
                .hasMessageContaining("required column \"Amount\" was not found")
                .hasMessageContaining("Supported names: Amount, Money, Value, Số tiền");
    }
}
