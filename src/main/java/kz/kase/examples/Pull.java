package kz.kase.examples;

import java.util.List;

import kz.bips.comps.utils.DateUtils;
import kz.bips.comps.utils.ExtendedPreferencesHocon;
import kz.bips.comps.utils.Log4JLoggerWrapper;

import kz.kase.iris.client.connectors.paho.PahoConnector;
import kz.kase.iris.client.rx.IrisRxClient;
import kz.kase.iris.model.IrisApiBase.Ohlc;
import kz.kase.iris.model.IrisApiCurtotals.CurTotal;
import kz.kase.iris.model.IrisApiTotals.Total;
import kz.kase.iris.model.IrisApiWatchlist.WatchlistInstrumentType;
import kz.kase.iris.model.IrisApiWatchlist.WatchlistRequest;
import kz.kase.iris.utils.IrisApiUtils;

/**
 * <p>Пример клиентского приложения для выыполнения запроса на получение списка наблюдения пользователя.</p>
 * <p><b>Created:</b> 22.07.2022 20:04:29</p>
 * @author victor
 */
public class Pull {
   protected static final Log4JLoggerWrapper log = new Log4JLoggerWrapper(Pull.class);

   public static void main(String[] args) {
      ExtendedPreferencesHocon props = AppProps.getProps();
      props.initLog4j();

      long t, start = log.time("start");

      try (IrisRxClient client = new IrisRxClient(new PahoConnector(props))) {
         t = log.time("open", start);

         // Формируем запрос на список наблюдения. В качестве оптимизации ограничиваем возвращаемый результат валютными инструментами и ценными бумагами.
         WatchlistRequest request = WatchlistRequest.newBuilder().addTypes(WatchlistInstrumentType.WIT_CURRENCIES).addTypes(WatchlistInstrumentType.WIT_SECURITIES).build();

         // Делаем запрос на поучение списка наблюдения пользователя.
         client.getWatchlistTopic().send(request).blockingSubscribe(reply -> {
            List<CurTotal> curQuotesList = reply.getCurQuotesList();
            if (!curQuotesList.isEmpty()) {
               log.debug("WATCHLIST (CURRENCIES):");
               for (CurTotal total : curQuotesList) {
                  Ohlc price = total.getPrice();
                  String cp = price.hasClose() ? IrisApiUtils.fromDecimal(price.getClose()).toPlainString() : "цена неизвестна";
                  String ct = price.hasCloseTime() ? IrisApiUtils.toLocalDateTime(price.getCloseTime()).format(DateUtils.DDMMYYYY_TIME) : "сегодня не торговался";
                  log.debug("   %s (%s): %s", total.getInstrumentCode(), ct, cp);
               }
            }
            List<Total> secQuotesList = reply.getSecQuotesList();
            if (!secQuotesList.isEmpty()) {
               log.debug("WATCHLIST (SECURITIES):");
               for (Total total : secQuotesList) {
                  Ohlc price = total.getPrice();
                  String cp = price.hasClose() ? IrisApiUtils.fromDecimal(price.getClose()).toPlainString() : "цена неизвестна";
                  String ct = price.hasCloseTime() ? IrisApiUtils.toLocalDateTime(price.getCloseTime()).format(DateUtils.DDMMYYYY_TIME) : "сегодня не торговался";
                  log.debug("   %s (%s): %s", total.getInstrumentCode(), ct, cp);
               }
            }
         }, error -> {
            log.logStackTrace(error, "Watchlist request");
         });

         t = log.time("load", t);

      } catch (Exception e) {
         log.logStackTrace(e, "main");
      }

      log.time("end", start);
   }
}
