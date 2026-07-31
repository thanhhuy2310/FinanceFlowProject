package com.financeflow.repository;
import com.financeflow.entity.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProviderRepository extends JpaRepository<Provider,Long> {
    List<Provider> findAllByOrderByNameAsc();
}
