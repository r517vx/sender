## Создать рассылку
POST `http://localhost:8080/api/campaigns`
``` json
{
  "name": "TEST-landing-1",
  "fromEmail": "hello@mobilica.ru",
  "replyToEmail": "hello@mobilica.ru"
}
```
В ответе от сервера будет id
Рассылка создаётся в состоянии `DRAFT` - черновик.
___
## Добавить e-mail в базу (получателя рассылки)
POST `http://localhost:8080/api/recipients`
```json
{
  "email": "solarev93@gmail.com",
  "firstName": "Владимир Игоревич",
  "company": "Test",
  "source": "manual"
}
```
В ответе от сервера будет id
___
## Активировать рассылку
POST `http://localhost:8080/api/campaigns/1/activate`
Рассылка перейдёт из состояния `DRAFT` в состояние `ACTIVE`
___
## Добавить к рассылке получателей
POST `http://localhost:8080/api/campaigns/1/enqueue`
```json
{
  "recipientIds": [7, 5, 6]
}
```
Вместо ids можно слать прямо emails, если ids нет
В ответе ничего нет `200 OK` или 500-я ошибка сервера *TODO: Исправить!* 
___
### После добавления получателей в рассылку и активации рассылки начинается рассылка по таймеру в соотвествии с настройками
___
## Отправить вручную
POST `http://localhost:8080/api/sender/run-once?batchSize=1`
```json
{
    "sent": 1,
    "retryScheduled": 0,
    "failedFinal": 0
}
```
