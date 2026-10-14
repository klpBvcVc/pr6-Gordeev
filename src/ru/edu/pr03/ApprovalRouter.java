package ru.edu.pr03;

/**
 * Маршрутизация служебной заявки (практическая работа №6).
 *
 * Порядок проверок, вариант 13: R1 (персональные данные) => R3 (сумма)
 * => R2 (риск HIGH) => R4 (срочная заявка) => R5 (стандарт).
 * В базовом задании R2 стоял перед R3, в варианте 13 они поменяны местами.
 */
public class ApprovalRouter {
    public static final long FINANCE_LIMIT = 500_000L;

    private static final long PRIORITY_AMOUNT_STEP = 100_000L;
    private static final int HIGH_RISK_PRIORITY_BONUS = 10;

    public ApprovalRoute route(long amount, boolean urgent, RiskLevel risk, boolean containsPersonalData) {
        checkArguments(amount, risk);
        if (requiresSecurityReview(containsPersonalData)) {
            return ApprovalRoute.SECURITY_REVIEW;
        }
        if (requiresFinanceReview(amount)) {
            return ApprovalRoute.FINANCE_REVIEW;
        }
        if (requiresDirectorReview(risk)) {
            return ApprovalRoute.DIRECTOR_REVIEW;
        }
        if (requiresFastTrack(amount, urgent)) {
            return ApprovalRoute.FAST_TRACK;
        }
        return ApprovalRoute.STANDARD;
    }

    /**
     * Часть Б варианта 13: числовой приоритет заявки для сортировки очереди.
     * amount / 100000 плюс 10, если риск HIGH. Чем больше, тем раньше обработка.
     */
    public int addPriorityScore(long amount, RiskLevel risk) {
        checkArguments(amount, risk);
        int score = Math.toIntExact(amount / PRIORITY_AMOUNT_STEP);
        if (risk == RiskLevel.HIGH) {
            score += HIGH_RISK_PRIORITY_BONUS;
        }
        return score;
    }

    private void checkArguments(long amount, RiskLevel risk) {
        if (amount < 0) {
            throw new IllegalArgumentException("Ошибка: amount must be >= 0, got " + amount);
        }
        if (risk == null) {
            throw new IllegalArgumentException("Ошибка: risk is null");
        }
    }

    private boolean requiresSecurityReview(boolean containsPersonalData) {
        return containsPersonalData;
    }

    private boolean requiresFinanceReview(long amount) {
        return amount >= FINANCE_LIMIT;
    }

    private boolean requiresDirectorReview(RiskLevel risk) {
        return risk == RiskLevel.HIGH;
    }

    private boolean requiresFastTrack(long amount, boolean urgent) {
        return urgent && amount < FINANCE_LIMIT;
    }
}
