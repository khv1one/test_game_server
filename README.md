
Used libraries:
-
Cats, Fs2, Http4s, PureConfig

Pros:
-
- Added a simple interface based on chat
- Added another game, without special rules, to demonstrate the ease of integration
- Support for any number of players in all games (Need to set lobby size)
- Easy integration of new games with custom rules
- The backend supports multi-tabling, but the frontend needs some work

Cons:
-
- The restriction on the negative balance of the player's account has not been implemented
- No mechanics for reconnecting after disconnecting a player
- Bug - no player uniqueness check when entering the lobby

Run 
-
**Application:**
```
sbt  "project gameServer" "run"
```
**Tests:**
```
sbt testOnly
```
Deploy
-
The project is deployed to heroku and available at
[link](https://fathomless-plains-04646.herokuapp.com)

-----------------------

Используемые библиотеки:
-
Cats, Fs2, Http4s, PureConfig

Плюсы:
-
- Добавлен простой интерфейс, основанный на чате
- Добавлена еще одна игра, без особых правил, для демонстрации простоты интеграции
- Поддержка любого количества игроков во всех играх (Указывается в коде при инициализации сервера)
- Простая интеграция новых игр с индивидуальными правилами
- Бекенд поддеривает мультитейблинг, но для демонстрации требуется доработка фронта

Минусы:
-
- Не реализовано ограничение на отрицательный остаток счета игрока
- Нет механики восстановления коннекта после отключения игрока
- Баг - нет проверки уникальности игрока при входе в лобби

Запуск:
-
**Приложения**
```
sbt  "project gameServer" "run"
```
**Тестов**:
```
sbt testOnly
```

Deploy
-
Проект задеплоин на heroku и доступен по
[ссылке](https://fathomless-plains-04646.herokuapp.com)