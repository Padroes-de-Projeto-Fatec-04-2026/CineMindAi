package br.com.fatec.cinemindai.movie.omdb;

import com.omertron.omdbapi.OmdbApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OmdbConfig {

    @Value("${omdb.api.key}")
    private String apiKey;

    /**
     * Instancia a API do OMDb do wrapper do Omertron.
     * Como estamos usando Dependency Injection (Spring), isso permite
     * que qualquer Advisor, Planner ou Service simplesmente injete OmdbApi.
     */
    @Bean
    public OmdbApi omdbApi() {
        if (apiKey == null || apiKey.trim().isEmpty() || "sua_chave_aqui".equals(apiKey)) {
            throw new IllegalStateException("A Chave da OMDb API não foi configurada! Verifique o arquivo .env");
        }
        return new OmdbApi(apiKey);
    }
}
