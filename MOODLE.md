# Запуск в moodle

## Предварительно
- поставить [node js](https://nodejs.org/en/)
- заполнить таблицы MySQL тестовыми данными
- проверить, что connectionString к MySQL стоит корректный в [настройках](https://github.com/procudin/CompPrehension/blob/master/src/main/resources/application.properties)
- поставитить мудл ([здесь](https://download.moodle.org/windows/) прям готовая сборка php+mariadb+apache) или использовать [тестовый стенд](http://edu.vstu.org)
- загрузить все зависимости -- ```npm install``` в папке с проектом

## Запуск сервера
- сбилдить фронт ```npm run build```
- сбилдить проект и запустить сервер spring

Если корректно настроен connection к бд, то сервер должен запуститься без ошибок.

## Настройка moodle

- создать теcтовый курс
- добавить в курс External tool
  ![2021-03-18_21-12-41](https://user-images.githubusercontent.com/20419403/111676191-fe39b500-882e-11eb-8968-8caec1903dbb.png)
- добавить новый источник и настроить его
  ![2021-03-18_21-16-52](https://user-images.githubusercontent.com/20419403/111676566-5c669800-882f-11eb-97c8-7572b4372197.png)
  ![2021-03-18_21-20-27](https://user-images.githubusercontent.com/20419403/111677578-68068e80-8830-11eb-8976-b8396e1c2dec.png)

## Примечание

Для примера добавил ExternalTool'a на [http://edu.vstu.org/](http://edu.vstu.org/course/view.php?id=15). 

Нужно также учитывать, что если настраивать связь с сервером из интернета (например с edu.vstu.org) есть проблема с работой в Google Chrome v80+. Все из-за того, что хром последних версий не сохраняет куки с небезопасных ресурсов (http). Тут либо настроить ssl на сервере, либо использовать другой браузер (на Firefox норм открывает).
