package kz.kase.examples;

import java.util.ArrayList;
import java.util.List;

import kz.bips.comps.utils.ExtendedPreferencesHocon;
import kz.bips.comps.utils.Log4JLoggerWrapper;

import kz.kase.iris.client.connectors.paho.PahoConnector;
import kz.kase.iris.client.rx.IrisRxClient;
import kz.kase.iris.model.IrisApiBase.Language;
import kz.kase.iris.model.IrisApiWatchlist.WatchlistAddRequest;
import kz.kase.iris.model.IrisApiWatchlist.WatchlistInstrument;
import kz.kase.iris.model.IrisApiWatchlist.WatchlistInstrumentType;
import kz.kase.iris.model.IrisApiWatchlist.WatchlistSearchRequest;

/**
 * <p>Пример клиентского приложения для добавления валютных инструментов в список наблюдения пользователя.</p>
 * <p><b>Created:</b> 22.07.2022 20:05:00</p>
 * @author victor
 */
public class Subscribe {
   private static final Log4JLoggerWrapper log = new Log4JLoggerWrapper(Subscribe.class);

   /**
    * Список кодов инструментов для добавления в список наблюдения.
    */
   private static final List<String> CODES = List.of("CNYKZT_TOD", "CNYKZT_TOM", "USDKZT_TOD", "USDKZT_TOM");

   public static void main(String[] args) {
      ExtendedPreferencesHocon props = AppProps.getProps();
      props.initLog4j();

      long t, start = log.time("start");

      try (IrisRxClient client = new IrisRxClient(new PahoConnector(props))) {
         t = log.time("open", start);

         List<WatchlistInstrument> instrumentsList = new ArrayList<>();

         // Выполянем поиск инструментов для добавления в список по коду инструмента.

         for (String code : CODES) {
            log.debug("SEARCH: %s", code);
            client.getWatchlistSearchTopic().send(WatchlistSearchRequest.newBuilder().setCode(code).setLang(Language.RU).build()).blockingSubscribe(reply -> {
               for (WatchlistInstrument instrument : reply.getInstrumentsList()) {
                  // На всякий случай проверяем, что найденный инструмент является валютным.
                  if (instrument.getType() == WatchlistInstrumentType.WIT_CURRENCIES) {
                     instrumentsList.add(instrument);
                  }
               }
            }, error -> {
               log.error("ERROR: %s", error.getMessage());
            });
         }

         t = log.time("search", t);

         // Выводим список для контроля.

         log.debug("INSTUMENTS:");
         for (WatchlistInstrument r : instrumentsList) {
            log.debug("  %s(%d): %s", r.getCode(), r.getInstrumentId(), r.getType().toString());
         }

         // Добавление инструментов в список наблюдения пользователя.

         client.getWatchlistAddTopic().send(WatchlistAddRequest.newBuilder().addAllInstruments(instrumentsList).build()).blockingSubscribe(reply -> {
            log.debug("ADD: OK");
         }, error -> {
            log.error("ERROR: %s", error.getMessage());
         });

         t = log.time("add", t);

      } catch (Exception e) {
         log.logStackTrace(e, "main");
      }

      log.time("end", start);
   }
}
