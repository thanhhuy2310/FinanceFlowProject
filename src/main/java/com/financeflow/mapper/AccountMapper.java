package com.financeflow.mapper;

import com.financeflow.dto.request.account.AccountRequest;
import com.financeflow.dto.response.account.AccountResponse;
import com.financeflow.entity.Account;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "provider", ignore = true)
    @Mapping(target = "transactions", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Account toEntity(AccountRequest request);

    @Mapping(source = "provider.id", target = "providerId")
    AccountResponse toResponse(Account account);
}
