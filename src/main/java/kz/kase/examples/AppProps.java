package kz.kase.examples;

import kz.bips.comps.utils.ExtendedPreferencesHocon;

/**
 * <p>Настройки приложения.</p>
 * <p><b>Created:</b> 19.07.2022 19:20:30</p>
 * @author victor
 */
public class AppProps extends ExtendedPreferencesHocon {
   public static final String CONFIG_ID = "rx_watchlist";

   /**
   * Private constructor prevents instantiation from other classes.
   */
   private AppProps() {
   }

   /**
   * AppPropsHolder is loaded on the first execution of AppProps.getProps(), not before.
   */
   private static class AppPropsHolder {
      public static final AppProps INSTANCE = new AppProps();
   }

   @Override
   public String getApplicationNodeName() {
      return CONFIG_ID;
   }

   /**
   * <p>Configuration library for JVM languages (see https://github.com/typesafehub/config and https://m.habrahabr.ru/company/mailru/blog/306848/ for documentation).</p>
   * <p><b>Note:</b> Config is an immutable object and thus safe to use from multiple threads (see http://typesafehub.github.io/config/latest/api).</p>
   */
   public static ExtendedPreferencesHocon getProps() {
      return AppPropsHolder.INSTANCE;
   }

}