package com.financeflow.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The fields the CSV importer understands, with the column names (aliases) that
 * can be used for each field in an uploaded file.
 * <p>
 * Matching is case-insensitive and ignores leading/trailing whitespace, so
 * {@code " Amount "}, {@code "amount"} and {@code "AMOUNT"} all resolve to
 * {@link #AMOUNT}.
 * <p>
 * To support a new column name for an existing field, add it to that field's
 * alias list (e.g. {@code DATE("date", "Date", ..., "Ngày tháng")}). To add a
 * whole new field, add an enum constant and register it in
 * {@link HeaderMapper} if it should be mandatory.
 */
public enum CsvHeaderField {

    DATE("date", "Date", "Transaction Date", "Txn Date", "Ngày"),
    DESCRIPTION("description", "Description", "Detail", "Note", "Nội dung"),
    AMOUNT("amount", "Amount", "Money", "Value", "Số tiền"),
    CATEGORY("category", "Category", "Type", "Expense Category", "Danh mục"),
    TRANSACTION_TYPE("type", "Type", "Transaction Type", "Txn Type"),
    PROVIDER("provider", "Provider", "Bank", "Bank Name", "Ngân hàng"),
    REFERENCE("reference", "Reference", "Ref", "Transaction ID");

    private final String canonicalName;
    private final String displayName;
    private final List<String> displayAliases;
    private final List<String> normalizedAliases;

    /**
     * @param canonicalName the exact name used by the importer's own template
     *                      (e.g. {@code date}); matched case-insensitively
     * @param aliasNames    display names users may write in their CSV instead
     *                      (matched case-insensitively and ignoring surrounding
     *                      whitespace)
     */
    CsvHeaderField(String canonicalName, String... aliasNames) {
        this.canonicalName = canonicalName;
        this.displayName = canonicalName.substring(0, 1).toUpperCase(Locale.ROOT)
                + canonicalName.substring(1);
        this.displayAliases = List.of(aliasNames);
        this.normalizedAliases = List.of(aliasNames).stream()
                .map(alias -> alias.toLowerCase(Locale.ROOT))
                .toList();
    }

    /** The exact template name, lower case (e.g. {@code date}). */
    public String canonicalName() {
        return canonicalName;
    }

    /**
     * The alias names to match against uploaded headers, lower case.
     * Excludes the canonical name, which is resolved separately.
     */
    public List<String> aliases() {
        return normalizedAliases;
    }

    /**
     * Human readable list of every accepted name for this field,
     * used in error messages (e.g. {@code "Amount, Money, Value, Số tiền"}).
     */
    public String supportedNames() {
        List<String> names = new ArrayList<>();
        names.add(displayName);
        for (String alias : displayAliases) {
            if (!alias.equals(displayName)) {
                names.add(alias);
            }
        }
        return String.join(", ", names);
    }

    /**
     * A complete "column missing" message for this field, listing the supported
     * names so users can fix their header without looking anything up.
     */
    public String missingColumnMessage() {
        return "required column \"" + displayName + "\" was not found. Supported names: "
                + supportedNames();
    }
}
