package com.company.dto;

import java.math.BigDecimal;

public record TentativeBalanceDto(BigDecimal officialBalance,
                                  BigDecimal tentativeBalance,
                                  boolean unavailable,
                                  String message) {
}
