package com.financeflow.mapper;

import com.financeflow.dto.request.transaction.TransactionRequest;
import com.financeflow.dto.response.transaction.TransactionResponse;
import com.financeflow.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "importBatch", ignore = true)
    @Mapping(target = "reference", ignore = true)
    @Mapping(target = "attachmentUrl", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Transaction toEntity(TransactionRequest request);

    @Mapping(source = "account.id", target = "accountId")
    @Mapping(source = "account.accountName", target = "accountName")
    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "category.name", target = "categoryName")
    TransactionResponse toResponse(Transaction transaction);
}
