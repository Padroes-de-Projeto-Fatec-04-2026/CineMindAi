package br.com.fatec.cinemindai.movie;

import br.com.fatec.cinemindai.movie.omdb.OmdbService;
import com.omertron.omdbapi.model.SearchResults;
import com.omertron.omdbapi.model.OmdbVideoFull;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Primary
public class HybridMovieCatalogAdapter implements MovieCatalogPort {

    private final OmdbService omdbService;
    private final InMemoryMovieCatalogAdapter inMemoryFallback;
    
    // Cache de memória para evitar sobrecarga e requisições repetidas na API da OMDb
    private final Map<String, List<MovieSummary>> searchCache = new ConcurrentHashMap<>();
    private final Map<String, MovieDetails> detailsCache = new ConcurrentHashMap<>();

    public HybridMovieCatalogAdapter(OmdbService omdbService, InMemoryMovieCatalogAdapter inMemoryFallback) {
        this.omdbService = omdbService;
        this.inMemoryFallback = inMemoryFallback;
    }

    @Override
    public List<MovieSummary> searchByTitle(String title) {
        if (title == null || title.isBlank()) return List.of();
        
        String cacheKey = title.trim().toLowerCase();
        if (searchCache.containsKey(cacheKey)) {
            return searchCache.get(cacheKey);
        }

        try {
            Optional<SearchResults> results = omdbService.searchMovies(title);
            if (results.isPresent() && results.get().isResponse() && results.get().getResults() != null && !results.get().getResults().isEmpty()) {
                List<MovieSummary> summaries = results.get().getResults().stream()
                        .map(r -> new MovieSummary(r.getImdbID(), r.getTitle(), safeParseYear(r.getYear()), List.of(), "Sinopse não disponível na busca simples"))
                        .collect(Collectors.toList());
                searchCache.put(cacheKey, summaries);
                return summaries;
            }
        } catch (Exception e) {
            System.err.println("Erro ao buscar no OMDb, usando fallback de memória: " + e.getMessage());
        }
        
        List<MovieSummary> fallbackResults = inMemoryFallback.searchByTitle(title);
        searchCache.put(cacheKey, fallbackResults);
        return fallbackResults;
    }

    @Override
    public List<MovieSummary> recommendByGenre(String genre) {
        // OMDb API não suporta busca pura por gênero sem um título base
        return inMemoryFallback.recommendByGenre(genre);
    }

    @Override
    public List<MovieSummary> recommendSimilarTo(String title) {
        // OMDb API não suporta busca de filmes similares diretamente
        return inMemoryFallback.recommendSimilarTo(title);
    }

    @Override
    public MovieDetails getDetails(String titleOrId) {
        if (titleOrId == null || titleOrId.isBlank()) return null;
        
        String cacheKey = titleOrId.trim().toLowerCase();
        if (detailsCache.containsKey(cacheKey)) {
            return detailsCache.get(cacheKey);
        }

        try {
            Optional<OmdbVideoFull> result;
            
            // Otimização: Evitar 2 requests caso já saibamos que é um IMDb ID (tt seguido de números)
            if (cacheKey.matches("^tt\\d+$")) {
                result = omdbService.getMovieDetailsById(titleOrId, true);
            } else {
                result = omdbService.getMovieDetailsByTitle(titleOrId, true);
                if (result.isEmpty() || result.get().getTitle() == null) {
                    result = omdbService.getMovieDetailsById(titleOrId, true);
                }
            }
            
            if (result.isPresent() && result.get().getTitle() != null) {
                OmdbVideoFull movie = result.get();
                MovieDetails details = new MovieDetails(
                        movie.getImdbID(),
                        movie.getTitle(),
                        safeParseYear(movie.getYear()),
                        parseList(movie.getGenre()),
                        movie.getPlot(),
                        movie.getDirector(),
                        parseList(movie.getActors()),
                        safeParseDouble(movie.getImdbRating())
                );
                detailsCache.put(cacheKey, details);
                // Propagar para o cache via ID também, caso a busca tenha sido por título
                if (movie.getImdbID() != null) {
                    detailsCache.put(movie.getImdbID().toLowerCase(), details);
                }
                return details;
            }
        } catch (Exception e) {
            System.err.println("Erro ao buscar detalhes no OMDb, usando fallback de memória: " + e.getMessage());
        }
        
        MovieDetails fallbackDetails = inMemoryFallback.getDetails(titleOrId);
        if (fallbackDetails != null) {
            detailsCache.put(cacheKey, fallbackDetails);
        }
        return fallbackDetails;
    }

    private int safeParseYear(String yearStr) {
        try {
            if (yearStr == null || yearStr.isEmpty()) return 0;
            String numeric = yearStr.replaceAll("[^0-9]", "");
            if (numeric.length() >= 4) {
                return Integer.parseInt(numeric.substring(0, 4));
            }
            return Integer.parseInt(numeric);
        } catch (Exception e) {
            return 0;
        }
    }
    
    private double safeParseDouble(String val) {
        try {
            if (val == null || val.isEmpty() || "N/A".equalsIgnoreCase(val)) return 0.0;
            return Double.parseDouble(val);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private List<String> parseList(String commaSeparated) {
        if (commaSeparated == null || commaSeparated.isEmpty() || "N/A".equalsIgnoreCase(commaSeparated)) {
            return List.of();
        }
        return List.of(commaSeparated.split(",\\s*"));
    }
}
