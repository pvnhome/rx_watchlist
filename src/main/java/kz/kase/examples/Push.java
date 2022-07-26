package kz.kase.examples;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import kz.bips.comps.utils.DateUtils;
import kz.bips.comps.utils.ExtendedPreferencesHocon;
import kz.bips.comps.utils.Log4JLoggerWrapper;

import kz.kase.iris.client.connectors.paho.PahoConnector;
import kz.kase.iris.client.rx.IrisRxClient;
import kz.kase.iris.client.rx.api.watchlist.WatchlistTopic;
import kz.kase.iris.exceptions.IrisApiException;
import kz.kase.iris.model.IrisApiBase.Ohlc;
import kz.kase.iris.model.IrisApiCurtotals.CurTotal;
import kz.kase.iris.model.IrisApiWatchlist.WatchlistInstrumentType;
import kz.kase.iris.model.IrisApiWatchlist.WatchlistReply;
import kz.kase.iris.model.IrisApiWatchlist.WatchlistRequest;
import kz.kase.iris.utils.IrisApiUtils;

/**
 * <p>Пример клиентского приложения для получения обновлений списка наблюдения пользователя с сервера.</p>
 * <p><b>Created:</b> 25.07.2022 12:33:58</p>
 * @author victor
 */
public class Push {
   private static final Log4JLoggerWrapper log = new Log4JLoggerWrapper(Push.class);

   private static String userName;

   public static void main(String[] args) {
      ExtendedPreferencesHocon props = AppProps.getProps();
      props.initLog4j();

      userName = props.getString("mqtt.client.iris.user");

      log.info("start");

      try (IrisRxClient client = new IrisRxClient(new PahoConnector(props))) {
         log.debug("open");

         // Формируем запрос на список наблюдения. В качестве оптимизации ограничиваем возвращаемый результат валютными инструментами.
         WatchlistRequest request = WatchlistRequest.newBuilder().addTypes(WatchlistInstrumentType.WIT_CURRENCIES).build();

         WatchlistTopic watchlistTopic = client.getWatchlistTopic();

         // Делаем первоначальный синхронный (blockingSubscribe) запрос на поучение списка наблюдения пользователя.
         watchlistTopic.send(request).blockingSubscribe(Push::printWatchlist, error -> {
            log.logStackTrace(error, "Watchlist request");
         });

         // Подписываемся на получение обновлений списка наблидения в асинхронном режиме (subscribe).
         watchlistTopic.subscribe(Push::printWatchlist);

         client.getWatchlistUpdateTopic().subscribe(update -> {
            // Из документации (специфично для обновлений Watchlist)
            // Пользователи должны обновить список наблюдения в двух случаях: если они получили сообщение
            // WatchlistUpdate с пустым списком имен в параметре user_names или в случае присутствия своего
            // имени в user_names.
            if (update.getUserNamesCount() < 1) {
               // Список наблюдения должны обновить все без исключения пользователи.
               log.debug("update for all users");
               // Оправляем запрос асинхронно (sendAsync). 
               // Подписка на обновления была настроена выше (см. subscribe).
               watchlistTopic.sendAsync(request);
            } else if (isSameUser(update.getUserNamesList())) {
               // На сервере обновился список наблюдения подключенного в данный момент пользователя.
               log.debug("update for user %s", userName);
               watchlistTopic.sendAsync(request);
            } else {
               log.debug("update for another user");
            }
         });

         // В примере получаем обновления 10 минут.
         // В реальном приложении логика может быть другая.
         TimeUnit.MINUTES.sleep(10);

      } catch (Exception e) {
         log.logStackTrace(e, "main");
      }

      log.info("end");
   }

   private static boolean isSameUser(List<String> userNames) {
      for (String name : userNames) {
         if (userName.equals(name)) {
            return true;
         }
      }
      return false;
   }

   private static void printWatchlist(WatchlistReply reply) throws IrisApiException {
      log.debug("WATCHLIST(on %s):", LocalDateTime.now().format(DateUtils.DDMMYYYY_TIME));
      for (CurTotal total : reply.getCurQuotesList()) {
         Ohlc price = total.getPrice();
         String cp = price.hasClose() ? IrisApiUtils.fromDecimal(price.getClose()).toPlainString() : "цена неизвестна";
         String ct = price.hasCloseTime() ? IrisApiUtils.toLocalDateTime(price.getCloseTime()).format(DateUtils.DDMMYYYY_TIME) : "сегодня не торговался";
         log.debug("   %s (%s): %s", total.getInstrumentCode(), ct, cp);
      }
   }

}
