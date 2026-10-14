package ru.edu.pr03;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

/**
 * Единственная точка входа и тестовый раннер для ApprovalRouter.
 *
 * Режим 1: unit-проверки в коде. Режим 2: data-driven из data/sample.csv.
 * Режим 3 (по ключу --interactive): ввод заявок с консоли, как на защите.
 */
public class SelfCheck {
    private static final String SAMPLE_FILE = "data/sample.csv";
    private static final String SAMPLE_HEADER = "amount;urgent;risk;personal;expected";
    private static final String ERROR_MARK = "ERROR";

    private static int total = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        runUnitChecks();
        runDataDriven();
        System.out.println("Итого: " + (total - failed) + "/" + total + " проверок пройдено");
        if (args.length > 0 && args[0].equals("--interactive")) {
            runInteractive();
        }
        if (failed > 0) {
            System.exit(1);
        }
    }

    // ---------- режим 1: unit-проверки ----------

    private static void runUnitChecks() {
        checkIntersections();
        checkBasics();
        checkBoundaries();
        checkGuards();
        checkExtras();
        checkVariantPartA();
        checkVariantPartB();
    }

    // route, пересечения правил (раздел 3.2): какое правило сильнее
    private static void checkIntersections() {
        expectRoute("unit/1 personal приоритетнее urgent", 10_000, true, RiskLevel.LOW, true, ApprovalRoute.SECURITY_REVIEW);
        expectRoute("unit/2 HIGH приоритетнее urgent", 10_000, true, RiskLevel.HIGH, false, ApprovalRoute.DIRECTOR_REVIEW);
        expectRoute("unit/3 personal приоритетнее HIGH", 10_000, false, RiskLevel.HIGH, true, ApprovalRoute.SECURITY_REVIEW);
        expectRoute("unit/4 сумма приоритетнее urgent", 600_000, true, RiskLevel.LOW, false, ApprovalRoute.FINANCE_REVIEW);
    }

    // route, по одному типовому случаю на каждый маршрут
    private static void checkBasics() {
        expectRoute("unit/5 FAST_TRACK", 10_000, true, RiskLevel.LOW, false, ApprovalRoute.FAST_TRACK);
        expectRoute("unit/6 FINANCE_REVIEW на границе 500000", 500_000, false, RiskLevel.LOW, false, ApprovalRoute.FINANCE_REVIEW);
        expectRoute("unit/7 STANDARD", 10_000, false, RiskLevel.LOW, false, ApprovalRoute.STANDARD);
        expectRoute("unit/8 MEDIUM не активирует R2", 10_000, false, RiskLevel.MEDIUM, false, ApprovalRoute.STANDARD);
        expectRoute("unit/9 SECURITY_REVIEW", 10_000, false, RiskLevel.LOW, true, ApprovalRoute.SECURITY_REVIEW);
    }

    // route, граница FINANCE_LIMIT: 499999 / 500000 / 500001
    private static void checkBoundaries() {
        expectRoute("unit/10 499999 срочная => FAST_TRACK", 499_999, true, RiskLevel.LOW, false, ApprovalRoute.FAST_TRACK);
        expectRoute("unit/11 500001 => FINANCE_REVIEW", 500_001, false, RiskLevel.LOW, false, ApprovalRoute.FINANCE_REVIEW);
        expectRoute("unit/11b 500000 срочная => FINANCE_REVIEW", 500_000, true, RiskLevel.LOW, false, ApprovalRoute.FINANCE_REVIEW);
    }

    // route, защитные условия (раздел 3.4), проверяем и тип, и текст сообщения
    private static void checkGuards() {
        expectError("unit/12 amount < 0", -1, true, RiskLevel.LOW, false, "Ошибка: amount must be >= 0, got -1");
        expectError("unit/13 risk == null", 10_000, true, null, false, "Ошибка: risk is null");
    }

    // route, дополнительные проверки из раздела 8.5
    private static void checkExtras() {
        expectRoute("unit/14 amount = 0 => STANDARD", 0, false, RiskLevel.LOW, false, ApprovalRoute.STANDARD);
        expectRoute("unit/15 MEDIUM + urgent < лимита", 10_000, true, RiskLevel.MEDIUM, false, ApprovalRoute.FAST_TRACK);
        expectRoute("unit/16 все признаки сразу => R1", 700_000, true, RiskLevel.HIGH, true, ApprovalRoute.SECURITY_REVIEW);
    }

    // вариант 13, часть А: R3 проверяется раньше R2
    private static void checkVariantPartA() {
        expectRoute("var13-A/1 HIGH + 600000 => FINANCE_REVIEW", 600_000, false, RiskLevel.HIGH, false, ApprovalRoute.FINANCE_REVIEW);
        expectRoute("var13-A/2 HIGH + ровно 500000 => FINANCE_REVIEW", 500_000, false, RiskLevel.HIGH, false, ApprovalRoute.FINANCE_REVIEW);
        expectRoute("var13-A/3 HIGH + 499999 => DIRECTOR_REVIEW", 499_999, false, RiskLevel.HIGH, false, ApprovalRoute.DIRECTOR_REVIEW);
    }

    // вариант 13, часть Б: addPriorityScore
    private static void checkVariantPartB() {
        ApprovalRouter router = new ApprovalRouter();
        expectInt("var13-B/1 250000 LOW => 2", 2, router.addPriorityScore(250_000, RiskLevel.LOW));
        expectInt("var13-B/2 250000 HIGH => 12", 12, router.addPriorityScore(250_000, RiskLevel.HIGH));
        expectInt("var13-B/3 99999 LOW => 0", 0, router.addPriorityScore(99_999, RiskLevel.LOW));
        expectInt("var13-B/4 0 HIGH => 10", 10, router.addPriorityScore(0, RiskLevel.HIGH));
    }

    private static void expectRoute(String name, long amount, boolean urgent, RiskLevel risk,
                                    boolean personal, ApprovalRoute expected) {
        total++;
        try {
            ApprovalRoute actual = new ApprovalRouter().route(amount, urgent, risk, personal);
            report(name, actual == expected, "ожидалось " + expected + ", получено " + actual);
        } catch (RuntimeException e) {
            report(name, false, "исключение " + e.getMessage());
        }
    }

    private static void expectError(String name, long amount, boolean urgent, RiskLevel risk,
                                    boolean personal, String message) {
        total++;
        try {
            ApprovalRoute actual = new ApprovalRouter().route(amount, urgent, risk, personal);
            report(name, false, "ожидалось исключение, получено " + actual);
        } catch (IllegalArgumentException e) {
            report(name, message.equals(e.getMessage()), "сообщение: " + e.getMessage());
        }
    }

    private static void expectInt(String name, int expected, int actual) {
        total++;
        report(name, expected == actual, "ожидалось " + expected + ", получено " + actual);
    }

    private static void report(String name, boolean ok, String detail) {
        if (!ok) {
            failed++;
        }
        System.out.println((ok ? "PASS " : "FAIL ") + name + (ok ? "" : ": " + detail));
    }

    // ---------- режим 2: data-driven ----------

    private static void runDataDriven() {
        List<String> lines = readSample();
        if (lines == null) {
            return;
        }
        int number = 0;
        int passed = 0;
        for (String line : lines) {
            if (isSkippable(line)) {
                continue;
            }
            number++;
            if (reportDataLine(number, line)) {
                passed++;
            }
        }
        System.out.println("Data-driven: " + passed + "/" + number + " passed");
        total += number;
        failed += number - passed;
    }

    private static List<String> readSample() {
        try {
            return Files.readAllLines(Paths.get(SAMPLE_FILE), StandardCharsets.UTF_8);
        } catch (IOException e) {
            total++;
            report("data/файл", false, "не удалось прочитать " + SAMPLE_FILE);
            return null;
        }
    }

    private static boolean reportDataLine(int number, String line) {
        String problem = checkDataLine(line);
        if (problem == null) {
            System.out.println("PASS data/" + number);
            return true;
        }
        System.out.println("FAIL data/" + number + ": " + problem);
        return false;
    }

    private static boolean isSkippable(String line) {
        return line.isEmpty() || line.startsWith("#") || line.equals(SAMPLE_HEADER);
    }

    /** Возвращает null, если строка файла прошла, иначе причину провала. */
    private static String checkDataLine(String line) {
        String[] parts = line.split(";", -1);
        if (parts.length != 5) {
            return "ожидалось 5 значений, получено " + parts.length;
        }
        String expected = parts[4];
        Request request;
        try {
            request = Request.parse(parts);
        } catch (IllegalArgumentException e) {
            return expected.equals(ERROR_MARK) ? null : e.getMessage();
        }
        try {
            ApprovalRoute actual = new ApprovalRouter().route(
                    request.amount, request.urgent, request.risk, request.personal);
            if (expected.equals(ERROR_MARK)) {
                return "ожидалась ошибка, получено " + actual;
            }
            return actual.name().equals(expected) ? null : "ожидалось " + expected + ", получено " + actual;
        } catch (IllegalArgumentException e) {
            return expected.equals(ERROR_MARK) ? null : "неожиданное исключение: " + e.getMessage();
        }
    }

    // ---------- режим 3: интерактивный ----------

    private static void runInteractive() {
        System.out.println("Введите заявку: amount;urgent;risk;personal (exit или пустая строка - выход)");
        Scanner in = new Scanner(System.in);
        while (in.hasNextLine()) {
            String line = in.nextLine().replace("﻿", "");
            if (line.isEmpty() || line.equals("exit")) {
                break;
            }
            System.out.println(describe(line));
        }
    }

    private static String describe(String line) {
        String[] parts = line.split(";", -1);
        if (parts.length != 4) {
            return "Ошибка: ожидалось 4 значения, получено " + parts.length;
        }
        try {
            Request r = Request.parse(parts);
            return "Маршрут: " + new ApprovalRouter().route(r.amount, r.urgent, r.risk, r.personal);
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    // ---------- разбор строки заявки (первые 4 значения) ----------

    private static final class Request {
        final long amount;
        final boolean urgent;
        final RiskLevel risk;
        final boolean personal;

        private Request(long amount, boolean urgent, RiskLevel risk, boolean personal) {
            this.amount = amount;
            this.urgent = urgent;
            this.risk = risk;
            this.personal = personal;
        }

        static Request parse(String[] parts) {
            long amount = parseAmount(parts[0]);
            boolean urgent = parseFlag("urgent", parts[1]);
            RiskLevel risk = parseRisk(parts[2]);
            boolean personal = parseFlag("personal", parts[3]);
            return new Request(amount, urgent, risk, personal);
        }

        private static long parseAmount(String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Ошибка: amount не является целым числом: \"" + text + "\"");
            }
        }

        // Boolean.parseBoolean принимает любой мусор как false, поэтому проверяем строго
        private static boolean parseFlag(String name, String text) {
            if (text.equals("true")) {
                return true;
            }
            if (text.equals("false")) {
                return false;
            }
            throw new IllegalArgumentException("Ошибка: " + name + " должно быть true или false, получено \"" + text + "\"");
        }

        // слово null в файле означает «риск не передан», его отловит защитное условие route
        private static RiskLevel parseRisk(String text) {
            if (text.equals("null")) {
                return null;
            }
            try {
                return RiskLevel.valueOf(text);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Ошибка: risk должен быть LOW, MEDIUM или HIGH (регистр важен), получено \"" + text + "\"");
            }
        }
    }
}
