# Практическая работа №6

Небольшая логическая задача с ветвлениями и конечным набором решений.
Маршрутизация служебной заявки (`ApprovalRouter.route`), вариант 13.

Пакет называется `ru.edu.pr03`.

## Сборка и запуск

Нужна Java SE 21 или выше.

```bash
mkdir -p out
javac -encoding UTF-8 -d out $(find src -name "*.java")
java -cp out ru.edu.pr03.SelfCheck
```

Windows: `run_selfcheck.bat`.

Интерактивный режим (ввод заявок `amount;urgent;risk;personal`, выход по `exit` или пустой строке):

```bash
java -cp out ru.edu.pr03.SelfCheck --interactive
```

Если кириллица в консоли Windows отображается неправильно: `chcp 65001`
и `java -Dstdout.encoding=UTF-8 ...`.

## Состав

- `src/ru/edu/pr03` исходный код
- `data/sample.csv` контрольные значения (data-driven режим)
- `diagrams/` Activity-диаграмма и диаграмма классов (.dot и .png)
- `DECISIONS.md` решения и ответы на вопросы
