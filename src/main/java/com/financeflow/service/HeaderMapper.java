package com.financeflow.service;

import com.financeflow.exception.CsvImportException;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Resolves which column of an uploaded CSV header belongs to which
 * {@link CsvHeaderField}, so row parsing never has to search aliases again.
 * <p>
 * Matching rules:
 * <ul>
 *   <li>case-insensitive, ignores surrounding whitespace, strips a UTF-8 BOM;</li>
 *   <li>unknown columns are ignored, never treated as an error;</li>
 *   <li>every column maps to at most one field (first match wins);</li>
 *   <li>a column named exactly like a field's canonical template name always
 *       maps to that field, even if the word is also an alias of another field
 *       (e.g. {@code type} is the canonical transaction type column while
 *       {@code Type} alone falls back to the category alias);</li>
 *   <li>mandatory fields are validated here, before any data row is read, and
 *       the failure message lists every supported name for the missing column.</li>
 * </ul>
 * <p>
 * {@link #map} is called once per file with the header names, and its result
 * ({@link HeaderMapping}) carries the resolved column indexes for the whole
 * import.
 */
public final class HeaderMapper {

    private static final char BYTE_ORDER_MARK = '\uFEFF';

    /**
     * Fields that must have a matching column for the import to proceed.
     * Description, transaction type and reference remain optional at header
     * level (an absent type is derived from the amount sign, an absent
     * description is reported per row).
     */
    private static final Set<CsvHeaderField> REQUIRED_FIELDS = Set.of(
            CsvHeaderField.DATE,
            CsvHeaderField.AMOUNT,
            CsvHeaderField.CATEGORY,
            CsvHeaderField.PROVIDER);

    public HeaderMapping map(List<String> headerNames) {
        if (headerNames == null || headerNames.isEmpty()) {
            throw new CsvImportException("CSV file is missing the header row");
        }

        List<String> names = normalize(headerNames);
        List<String> lowerNames = names.stream()
                .map(name -> name.toLowerCase(Locale.ROOT))
                .toList();
        Map<CsvHeaderField, Integer> columnIndexes = new EnumMap<>(CsvHeaderField.class);
        boolean[] assigned = new boolean[names.size()];

        // Pass 1: exact canonical names (lower case) first, so a file that
        // follows the importer template always keeps its original meaning
        // (e.g. "type" stays the transaction type, not the category).
        for (CsvHeaderField field : CsvHeaderField.values()) {
            assignIfUnclaimed(field.canonicalName(), field, names, assigned, columnIndexes);
        }

        // Pass 2: match remaining columns against field aliases,
        // case-insensitively (the alias list is compared against the lower
        // cased header names). Fields are visited in declaration order, so a
        // shared alias such as "Type" resolves to the first field that
        // supports it (Category).
        for (CsvHeaderField field : CsvHeaderField.values()) {
            for (String alias : field.aliases()) {
                assignIfUnclaimed(alias, field, lowerNames, assigned, columnIndexes);
            }
        }

        validateRequiredColumns(columnIndexes);

        return new HeaderMapping(columnIndexes, names.size());
    }

    private List<String> normalize(List<String> headerNames) {
        List<String> normalized = new ArrayList<>(headerNames);

        if (normalized.get(0).startsWith(String.valueOf(BYTE_ORDER_MARK))) {
            normalized.set(0, normalized.get(0).substring(1));
        }
        normalized.replaceAll(header -> header.trim());

        return normalized;
    }

    private void assignIfUnclaimed(String name, CsvHeaderField field, List<String> headerNames,
                                   boolean[] assigned, Map<CsvHeaderField, Integer> columnIndexes) {
        if (columnIndexes.containsKey(field)) {
            return;
        }

        for (int index = 0; index < headerNames.size(); index++) {
            if (!assigned[index] && name.equals(headerNames.get(index))) {
                columnIndexes.put(field, index);
                assigned[index] = true;
                return;
            }
        }
    }

    private void validateRequiredColumns(Map<CsvHeaderField, Integer> columnIndexes) {
        List<String> missing = new ArrayList<>();

        for (CsvHeaderField field : CsvHeaderField.values()) {
            if (REQUIRED_FIELDS.contains(field) && !columnIndexes.containsKey(field)) {
                missing.add(field.missingColumnMessage());
            }
        }

        if (!missing.isEmpty()) {
            throw new CsvImportException("Invalid CSV header: " + String.join("; ", missing));
        }
    }

    /**
     * The resolved column positions for one CSV file.
     */
    public record HeaderMapping(Map<CsvHeaderField, Integer> columnIndexes, int columnCount) {

        public boolean has(CsvHeaderField field) {
            return columnIndexes.containsKey(field);
        }

        /** Column index of the given field, or {@code -1} when the column is absent. */
        public int indexOf(CsvHeaderField field) {
            return columnIndexes.getOrDefault(field, -1);
        }

        /** Number of columns in the header row (including unknown columns). */
        public int size() {
            return columnCount;
        }
    }
}
