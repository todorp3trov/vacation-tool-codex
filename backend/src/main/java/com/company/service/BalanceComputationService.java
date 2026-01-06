package com.company.service;

import com.company.dto.TentativeBalanceDto;
import com.company.integration.VacationBalanceClient;
import com.company.model.VacationRequestStatus;
import com.company.repos.VacationRequestRepository;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class BalanceComputationService {

    private final VacationBalanceClient vacationBalanceClient;
    private final BalanceSessionCache balanceSessionCache;
    private final VacationRequestRepository vacationRequestRepository;

    public BalanceComputationService(VacationBalanceClient vacationBalanceClient,
                                     BalanceSessionCache balanceSessionCache,
                                     VacationRequestRepository vacationRequestRepository) {
        this.vacationBalanceClient = vacationBalanceClient;
        this.balanceSessionCache = balanceSessionCache;
        this.vacationRequestRepository = vacationRequestRepository;
    }

    public TentativeBalanceDto computeForUser(UUID userId, HttpSession session) {
        BalanceSessionCache.BalanceSnapshot cached = balanceSessionCache.getSnapshot(session, userId);
        VacationBalanceClient.BalanceResult balanceResult;
        if (cached != null) {
            balanceResult = cached.unavailable()
                    ? VacationBalanceClient.BalanceResult.unavailable("cached unavailable")
                    : VacationBalanceClient.BalanceResult.available(cached.balance());
        } else {
            balanceResult = vacationBalanceClient.fetchBalance(userId);
            if (balanceResult.unavailable()) {
                balanceSessionCache.storeUnavailable(session, userId);
            } else {
                balanceSessionCache.store(session, userId, balanceResult.balance());
            }
        }
        return computeTentative(userId, balanceResult);
    }

    public TentativeBalanceDto computeForManagerView(UUID userId) {
        VacationBalanceClient.BalanceResult balanceResult = vacationBalanceClient.fetchBalance(userId);
        return computeTentative(userId, balanceResult);
    }

    private TentativeBalanceDto computeTentative(UUID userId, VacationBalanceClient.BalanceResult balanceResult) {
        BigDecimal official = balanceResult.unavailable() ? null : balanceResult.balance();
        BigDecimal tentative = null;
        if (official != null) {
            long pendingDays = vacationRequestRepository.sumDaysForStatus(userId, VacationRequestStatus.PENDING);
            tentative = official.subtract(BigDecimal.valueOf(pendingDays));
            if (tentative.compareTo(BigDecimal.ZERO) < 0) {
                tentative = BigDecimal.ZERO;
            }
        }
        return new TentativeBalanceDto(official, tentative, balanceResult.unavailable(), balanceResult.reason());
    }
}
