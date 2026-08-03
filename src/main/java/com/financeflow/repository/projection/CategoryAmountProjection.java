package com.financeflow.repository.projection;

import java.math.BigDecimal;

public interface CategoryAmountProjection {

    String getCategoryName();

    BigDecimal getTotalAmount();
}
