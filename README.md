# Пример использования IRIS API
В примере показана работа со списком наблюдения пользователя IRIS с помощью библиотеки `iris-api-java-client`. Использование `iris-api-java-client` является необязательным, но сильно упрощает работу с IRIS API.

Пример разделен на четыре части:

* Класс **Subscribe** - приложение для добавления валютных инструментов в список наблюдения пользователя.

* Класс **Pool** - приложение для выполнения единичного запроса на получение списка наблюдения пользователя. Подход применим если необходимо получать информацию от случая к случаю, например, когда пользователь нажимает на кнопку "Обновить".

* Класс **Push** - приложение для получения обновлений списка наблюдения пользователя с сервера. Подход применим если необходимо получать обновления на постоянной основе.

* Класс **Unsubscribe** - приложение для удаления всех валютных инструментов из списка наблюдения пользователя.

При работе со списком наблюдения следует учитывать, что сутки разделены на три периода:

* с 03:00 до начала следующей торговой сесии возвращаются данные только в поле "Тикер". Все остальные поля возвращаются пустыми.

* С начала до окончания сессии для иструментов из списка наблюдения по которым уже были сделки возвращаются данные во всех полях, а для инструментов по которым в рамках текущей сесии сделок не было поля возвращаются пустыми.

* С окончания сессии и до 03:00 следующего дня список наблюдения не меняется и возвращается в таком же виде в каком он был на момент завершения сессии.


## Запуск приложения

Поместите предоставленный вам файл `mqtt_servers.conf` в папку `conf`. Для выполнения единичного запроса запустите приложение из командной строки, используя Maven:

```
mvn -Dmaven.test.skip=true clean compile exec:java
```

## Настройки

Файлы настроек (папка conf):

* rx_watchlist_extended_props.conf - настройки приложения.

* mqtt_servers.conf - параметры для доступа к серверам (файл предоставляется отдельно);

* rx_watchlist_log4j.properties - настройки логирования (Log4j);

* ssl/letsencrypt_root_1.jks - корневые сертификаты (при работе по протоколу wss);

## Используемые библиотеки

Библиотеки в локальном репозитарии (папка lib):
* base_utils - базовые классы используемые в других библиотеках (обязательная);
* hocon_conf - работа с файлами настроек в формате [hocon](https://github.com/lightbend/config/blob/main/HOCON.md) (требуется только при работе с настройками из файлов, альтернативно параметры соединения могут быть заданы непосредственно в java-коде);
* iris-api-java - классы для работы с сообщеними IRIS API в формате [Protocol Buffers](https://developers.google.com/protocol-buffers) (обязательная);
* iris-api-java-client - клиентская библиотека для работы c IRIS API, использующая [RxJava](https://github.com/ReactiveX/RxJava) (необязательная);
* iris-api-java-utils - вспомогательная библиотека для преобразования типов IRIS API к типам Java (необязательная);

Общедоступные библиотеки:
* config - только при работе с форматом [hocon](https://github.com/lightbend/config/blob/main/HOCON.md);
* org.eclipse.paho.client.mqttv3 - клиентская библиотека [Paho](https://www.eclipse.org/paho/) для работы с [MQTT](https://mqtt.org/) (обязательная);
* protobuf-java - библиотека [Protocol Buffers](https://developers.google.com/protocol-buffers) (обязательная);
* reactive-streams и rxjava - библиотека [RxJava](https://github.com/ReactiveX/RxJava) (обязательна при использовании `iris-api-java-client`);
