package com.financeflow.service;

import com.financeflow.exception.CsvImportException;
import com.financeflow.service.HeaderMapper.HeaderMapping;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HeaderMapperTest {

    private final HeaderMapper headerMapper = new HeaderMapper();

    @Test
    void map_canonicalHeader_mapsEveryField() {
        HeaderMapping mapping = headerMapper.map(
                List.of("date", "description", "amount", "type", "category", "provider"));

        assertThat(mapping.indexOf(CsvHeaderField.DATE)).isEqualTo(0);
        assertThat(mapping.indexOf(CsvHeaderField.DESCRIPTION)).isEqualTo(1);
        assertThat(mapping.indexOf(CsvHeaderField.AMOUNT)).isEqualTo(2);
        assertThat(mapping.indexOf(CsvHeaderField.TRANSACTION_TYPE)).isEqualTo(3);
        assertThat(mapping.indexOf(CsvHeaderField.CATEGORY)).isEqualTo(4);
        assertThat(mapping.indexOf(CsvHeaderField.PROVIDER)).isEqualTo(5);
        assertThat(mapping.size()).isEqualTo(6);
    }

    @Test
    void map_aliasesCaseInsensitiveAndSurroundingSpaces_resolved() {
        HeaderMapping mapping = headerMapper.map(
                List.of(" Transaction Date ", "money", "Bank Name", "Type", " Note ", "REF"));

        assertThat(mapping.indexOf(CsvHeaderField.DATE)).isEqualTo(0);
        assertThat(mapping.indexOf(CsvHeaderField.AMOUNT)).isEqualTo(1);
        assertThat(mapping.indexOf(CsvHeaderField.PROVIDER)).isEqualTo(2);
        assertThat(mapping.indexOf(CsvHeaderField.CATEGORY)).isEqualTo(3);
        assertThat(mapping.indexOf(CsvHeaderField.DESCRIPTION)).isEqualTo(4);
        assertThat(mapping.indexOf(CsvHeaderField.REFERENCE)).isEqualTo(5);
        assertThat(mapping.has(CsvHeaderField.TRANSACTION_TYPE)).isFalse();
    }

    @Test
    void map_vietnameseAliases_resolved() {
        HeaderMapping mapping = headerMapper.map(
                List.of("Ngày", "Số tiền", "Danh mục", "Ngân hàng", "Nội dung"));

        assertThat(mapping.indexOf(CsvHeaderField.DATE)).isEqualTo(0);
        assertThat(mapping.indexOf(CsvHeaderField.AMOUNT)).isEqualTo(1);
        assertThat(mapping.indexOf(CsvHeaderField.CATEGORY)).isEqualTo(2);
        assertThat(mapping.indexOf(CsvHeaderField.PROVIDER)).isEqualTo(3);
        assertThat(mapping.indexOf(CsvHeaderField.DESCRIPTION)).isEqualTo(4);
    }

    @Test
    void map_unknownColumns_areIgnored() {
        HeaderMapping mapping = headerMapper.map(
                List.of("date", "location", "phone", "customer", "amount", "type", "category", "provider"));

        assertThat(mapping.indexOf(CsvHeaderField.DATE)).isEqualTo(0);
        assertThat(mapping.indexOf(CsvHeaderField.AMOUNT)).isEqualTo(4);
        assertThat(mapping.indexOf(CsvHeaderField.TRANSACTION_TYPE)).isEqualTo(5);
        assertThat(mapping.indexOf(CsvHeaderField.CATEGORY)).isEqualTo(6);
        assertThat(mapping.indexOf(CsvHeaderField.PROVIDER)).isEqualTo(7);
        assertThat(mapping.size()).isEqualTo(8);
    }

    @Test
    void map_missingRequiredColumn_throwsWithSupportedNames() {
        assertThatThrownBy(() -> headerMapper.map(
                List.of("date", "description", "type", "category", "provider")))
                .isInstanceOf(CsvImportException.class)
                .hasMessageContaining("Invalid CSV header")
                .hasMessageContaining("required column \"Amount\" was not found")
                .hasMessageContaining("Supported names: Amount, Money, Value, Số tiền");
    }

    @Test
    void map_multipleMissingRequiredColumns_listsEveryMissingField() {
        assertThatThrownBy(() -> headerMapper.map(
                List.of("date", "description", "amount", "type")))
                .isInstanceOf(CsvImportException.class)
                .hasMessageContaining("required column \"Category\" was not found")
                .hasMessageContaining("required column \"Provider\" was not found");
    }

    @Test
    void map_canonicalTypeColumn_mapsToTransactionTypeNotCategory() {
        HeaderMapping mapping = headerMapper.map(
                List.of("date", "description", "amount", "type", "category", "provider"));

        assertThat(mapping.indexOf(CsvHeaderField.TRANSACTION_TYPE)).isEqualTo(3);
        assertThat(mapping.indexOf(CsvHeaderField.CATEGORY)).isEqualTo(4);
    }

    @Test
    void map_typeOnlyColumn_mapsToCategory() {
        HeaderMapping mapping = headerMapper.map(
                List.of("Transaction Date", "Money", "Bank", "Type"));

        assertThat(mapping.indexOf(CsvHeaderField.DATE)).isEqualTo(0);
        assertThat(mapping.indexOf(CsvHeaderField.AMOUNT)).isEqualTo(1);
        assertThat(mapping.indexOf(CsvHeaderField.PROVIDER)).isEqualTo(2);
        assertThat(mapping.indexOf(CsvHeaderField.CATEGORY)).isEqualTo(3);
        assertThat(mapping.has(CsvHeaderField.TRANSACTION_TYPE)).isFalse();
    }

    @Test
    void map_bomPrefixedFirstColumn_handled() {
        HeaderMapping mapping = headerMapper.map(
                List.of("\uFEFFdate", "description", "amount", "type", "category", "provider"));

        assertThat(mapping.indexOf(CsvHeaderField.DATE)).isEqualTo(0);
    }

    @Test
    void map_emptyHeaderNames_throws() {
        assertThatThrownBy(() -> headerMapper.map(List.of()))
                .isInstanceOf(CsvImportException.class)
                .hasMessageContaining("missing the header row");
    }
}
