# Подключение LMS по LTI 1.3

LTI 1.3 — договорённость двух сторон: LMS (платформа) и CompPrehension (инструмент). Каждая сторона
подписывает свои сообщения своим ключом и знает открытый ключ другой:

- LMS подписывает id_token запуска — мы проверяем его ключом LMS;
- мы подписываем запрос токена для оценок и ответ deep linking — LMS проверяет их нашим ключом.

Ниже — на примере Moodle.

## 1. Moodle: зарегистрировать инструмент

Администрирование → Плагины → Внешний инструмент → «Управление инструментами» →
«настроить инструмент вручную».

| поле | значение |
|---|---|
| Tool URL | `<сервер>/lti/1_3/exercise` |
| LTI version | LTI 1.3 |
| Public key type | **Keyset URL** (см. ниже) |
| Public keyset | `<сервер>/lti/1_3/jwks` |
| Initiate login URL | `<сервер>/lti/1_3/login` |
| Redirection URI(s) | `<сервер>/lti/1_3/exercise`, `<сервер>/lti/1_3/exercise-settings`, `<сервер>/lti/1_3/configure-course` — по одному на строку |
| Tool configuration usage | показывать в выборе активностей |
| Default launch container | New window |
| Supports Deep Linking | да |
| Content Selection URL | `<сервер>/lti/1_3/configure-course` |
| IMS LTI Assignment and Grade Services | Use this service for grade sync and column management |
| Share launcher's name / email | Always — **без email пользователя не пустит** |
| Accept grades from the tool | Always |
| Custom parameters | пусто |

Custom parameters на уровне инструмента не задавайте: они перекрывают параметры активностей, и все
активности откроют одно и то же упражнение.

После сохранения значок «View configuration details» у инструмента покажет **Platform ID** (адрес
Moodle) и **Client ID** — они нужны на нашей стороне.

### Наш ключ: Keyset URL или RSA key

**Keyset URL — основной вариант.** Moodle сам забирает наш открытый ключ с `<сервер>/lti/1_3/jwks`,
копировать ключ руками не нужно, а после смены ключа на нашей стороне Moodle возьмёт новый оттуда же.
Условие: сервер Moodle должен достучаться до нашего `/lti/1_3/jwks` по HTTPS.

**RSA key — запасной вариант**, когда Moodle до нашего сервера не достаёт (например, сервер запущен
локально). Тогда в поле Public key вставляется открытый ключ в PEM (`-----BEGIN PUBLIC KEY-----…`),
парный нашему приватному. При смене ключа его придётся заменить в Moodle вручную.

Открытый ключ из приватного:

```bash
openssl pkey -in tool.pem -pubout
```

## 2. CompPrehension: добавить регистрацию

Регистрация — это переменные окружения сервера, `<ИМЯ>` — любое латиницей, оно же `kid` нашего ключа:

```
COMPPREHENSION_LTI_REGISTRATIONS_<ИМЯ>_ISSUER_URL=<Platform ID>
COMPPREHENSION_LTI_REGISTRATIONS_<ИМЯ>_CLIENT_ID=<Client ID>
COMPPREHENSION_LTI_REGISTRATIONS_<ИМЯ>_PRIVATE_KEY_PKCS8_BASE64=<наш приватный ключ>
```

- `ISSUER_URL` должен **побуквенно** совпадать с Platform ID: тот же протокол, без `/` на конце.
- Ключ LMS наш сервер берёт сам с `<ISSUER_URL>/mod/lti/certs.php` (адрес Moodle). У LMS с другим
  адресом ключей его задают явно: `…_PLATFORM_JWKS_URL=<адрес>`. Если у LMS нет JWKS, вместо адреса
  можно указать её открытый ключ: `…_PLATFORM_PUBLIC_KEY_BASE64=<X.509 DER в base64>`; оба сразу
  задавать нельзя.

Новый ключ инструмента:

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out tool.pem
```

```bash
openssl pkcs8 -topk8 -nocrypt -in tool.pem -outform DER | base64 -w0
```

После перезапуска проверьте, что `<сервер>/lti/1_3/jwks` отдаёт ключ с `kid` = `<ИМЯ>`.

Запуск принимается только от зарегистрированной LMS: подпись id_token, `iss`, `aud` (= наш Client ID)
и срок действия проверяются при каждом запуске. Первый такой запуск создаёт LMS у нас доверенной.
LMS со статусом `UNTRUSTED` или `BANNED` в `education_resource` не пускается, даже если она
зарегистрирована.

## 3. Активности в курсе

Активности создаёт преподаватель через deep linking: «Добавить активность» → наш инструмент →
«Select content». В окне выбора два раздела, каждый отправляет в Moodle свою активность:

- **Настройка упражнений курса** — активность для преподавателя, которая открывает страницу упражнений
  курса у нас (`/lti/1_3/exercise-settings`). Оценок у неё нет; скройте её от студентов в Moodle.
- **Упражнения** — по активности на каждое выбранное упражнение курса, с параметром `exercise_id`
  и колонкой оценок.

В новом курсе упражнений ещё нет, поэтому сначала добавляется активность настройки: в ней упражнения
создают или импортируют, и только потом их выбирают во втором разделе.

## 4. Оценки

Оценка уходит в Moodle, когда студент завершает попытку. Moodle принимает оценки только участников
курса с ролью, которая попадает в журнал (по умолчанию «Студент»). Проверять под администратором
с переключённой ролью бесполезно — Moodle ответит `400 Incorrect score received`; нужен настоящий
студент, записанный в курс.

## Если не работает

| симптом | причина |
|---|---|
| 403 на `/lti/1_3/login` | LMS не зарегистрирована или Client ID не совпадает |
| 403 на запуске: `Invalid LTI id_token` | неверный `ISSUER_URL`, Client ID или ключ LMS недоступен |
| 403 на запуске: `does not match a login` | запуск пришёл в другую сессию: браузер не отправил cookie (открывайте в новом окне, сервер — только по HTTPS) |
| 403 на запуске: `is not trusted` | LMS в базе `UNTRUSTED` или `BANNED` |
| `id_token must contain non-empty email claim` | в инструменте не включена передача email |
| открывается не то упражнение | задан `exercise_id` в Custom parameters инструмента |
| `400 Incorrect score received` | оценку получает не студент курса |
