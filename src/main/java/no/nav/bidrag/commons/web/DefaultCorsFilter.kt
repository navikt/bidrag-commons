package no.nav.bidrag.commons.web;

import java.util.List;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

public class DefaultCorsFilter extends CorsFilter {

  public static UrlBasedCorsConfigurationSource getDefaultCorsConfiguration(){
    CorsConfiguration corsConfiguration = new CorsConfiguration();
    corsConfiguration.setAllowedOrigins(List.of("*"));
    corsConfiguration.setAllowedHeaders(List.of("Content-Type", "Authorization", "Content-Length", "X-Requested-With", "X-Correlation-ID", "X-Enhet", "Nav-Call-Id", "Nav-Consumer-Id"));
    corsConfiguration.setExposedHeaders(List.of("Warning", "X-Enhet", "X-Correlation-Id"));
    corsConfiguration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
    UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource = new UrlBasedCorsConfigurationSource();
    urlBasedCorsConfigurationSource.registerCorsConfiguration("/**", corsConfiguration);
    return urlBasedCorsConfigurationSource;
  }

  public DefaultCorsFilter() {
    super(DefaultCorsFilter.getDefaultCorsConfiguration());
  }

}