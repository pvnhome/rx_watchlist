package kz.kase.examples;

import java.util.ArrayList;
import java.util.List;

import kz.bips.comps.utils.ExtendedPreferencesHocon;
import kz.bips.comps.utils.Log4JLoggerWrapper;

import kz.kase.iris.client.connectors.paho.PahoConnector;
import kz.kase.iris.client.rx.IrisRxClient;
import kz.kase.iris.model.IrisApiBase.Language;
import kz.kase.iris.model.IrisApiCurtotals.CurTotal;
import kz.kase.iris.model.IrisApiWatchlist.WatchlistDelRequest;
import kz.kase.iris.model.IrisApiWatchlist.WatchlistInstrument;
import kz.kase.iris.model.IrisApiWatchlist.WatchlistInstrumentType;
import kz.kase.iris.model.IrisApiWatchlist.WatchlistRequest;
import kz.kase.iris.model.IrisApiWatchlist.WatchlistSearchRequest;

/**
 * <p>Пример клиентского приложения для удаления всех валютных инструментов из списока наблюдения пользователя.</p>
 * <p><b>Created:</b> 25.07.2022 12:10:39</p>
 * @author victor
 */
public class Unsubscribe {
   protected static final Log4JLoggerWrapper log = new Log4JLoggerWrapper(Unsubscribe.class);

   public static void main(String[] args) {
      ExtendedPreferencesHocon props = AppProps.getProps();
      props.initLog4j();

      long t, start = log.time("start");

      try (IrisRxClient client = new IrisRxClient(new PahoConnector(props))) {
         t = log.time("open", start);

         List<String> instrumentsCodes = new ArrayList<>();
         List<WatchlistInstrument> instrumentsList = new ArrayList<>();

         // Один из самых простых способов получить список инструментов - это сделать запрос на список наблюдения.  

         WatchlistRequest request = WatchlistRequest.newBuilder().addTypes(WatchlistInstrumentType.WIT_CURRENCIES).build();

         client.getWatchlistTopic().send(request).blockingSubscribe(reply -> {
            if (reply.getCurQuotesCount() > 0) {
               log.debug("WATCHLIST:");
               for (CurTotal total : reply.getCurQuotesList()) {
                  instrumentsCodes.add(total.getInstrumentCode());
                  log.debug("   %s", total.getInstrumentCode());
               }
            }
         }, error -> {
            log.logStackTrace(error, "Watchlist request");
         });

         t = log.time("load", t);

         // Выполянем поиск инструментов для удаления из списка по коду инструмента.

         for (String code : instrumentsCodes) {
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

         if (!instrumentsList.isEmpty()) {
            // Выводим список для контроля.

            log.debug("INSTUMENTS:");
            for (WatchlistInstrument r : instrumentsList) {
               log.debug("  %s(%d): %s", r.getCode(), r.getInstrumentId(), r.getType().toString());
            }

            // Удаление инструментов из списка наблюдения пользователя.

            client.getWatchlistDelTopic().send(WatchlistDelRequest.newBuilder().addAllInstruments(instrumentsList).build()).blockingSubscribe(reply -> {
               log.debug("DEL: OK");
            }, error -> {
               log.error("ERROR: %s", error.getMessage());
            });

            t = log.time("del", t);
         }

      } catch (Exception e) {
         log.logStackTrace(e, "main");
      }

      log.time("end", start);
   }
}
