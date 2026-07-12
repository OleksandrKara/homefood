# homefood

Маркетплейс домашней еды — V1.

Полностью изолированный проект на VPS, отдельный от остальных приложений
(`akluxnails-home`, `salaryReview`, `salonLandings`), которые относятся
к другому бизнесу (beauty salon, akluxnails.com).

## Изоляция

- Своя папка: `/home/ubuntu/homefood/`
- Свой Postgres instance: контейнер `homefood-postgres`, своя Docker-сеть
  `homefood_default`, свой volume `homefood_postgres-data`.
  **Не** подключён к сети `salaryreview_default`, никакого общего
  доступа к БД salon-приложений.
- Порт Postgres наружу не публикуется — доступ только из контейнеров
  внутри `homefood_default`.

## Приложение

`admin/` — Spring Boot 4 (Java 21) + Thymeleaf admin-панель (V1):
клиенты, товары, заказы (самовывоз/доставка). Один общий admin-логин
(env `ADMIN_USERNAME`/`ADMIN_PASSWORD`), Flyway-миграции, Postgres из
этого же compose-файла. Работает под context-path `/admin`.

Живёт на https://food.akluxnails.com/admin — деплой через
`.github/workflows/deploy.yml` (push в `main` → SSH на VPS →
`docker compose up -d --build`, без blue/green: это внутренний
инструмент, не ads-трафик, кратковременный рестарт при деплое допустим).

## Разработка

```bash
cp .env.example .env   # заполнить реальные значения
docker compose up -d
```
