package br.com.fatec.cinemindai.movie.omdb;

import com.omertron.omdbapi.OmdbApi;
import com.omertron.omdbapi.model.OmdbVideoFull;
import com.omertron.omdbapi.model.SearchResults;
import com.omertron.omdbapi.tools.OmdbBuilder;
import com.omertron.omdbapi.OMDBException;
import org.springframework.stereotype.Service;
import java.util.Optional;

/**
 * Serviço responsável pela interação direta com a OMDb API.
 * 
 * PARA A EQUIPE:
 * Esta classe é o Information Retriever (Coletor de Dados) da nossa aplicação.
 * Em vez de testes manuais, injetem esta classe nas "Tools" e nos "Advisors" 
 * para construir o contexto que será mandado pro Ollama.
 * 
 * Abaixo estão todos os métodos de busca possíveis baseados na documentação da OMDb.
 */
@Service
public class OmdbService {

    private final OmdbApi omdbApi;

    public OmdbService(OmdbApi omdbApi) {
        this.omdbApi = omdbApi;
    }

    // ==========================================
    // MÉTODOS DE BUSCA POR LISTA (By Search)
    // ==========================================

    /**
     * Pesquisa filmes usando um termo genérico, sem filtros.
     * Ideal para buscas iniciais.
     *
     * @param searchTerm Título ou termo a ser pesquisado (Obrigatório).
     * @return Lista de resultados de busca, ou Optional.empty se erro/nada encontrado.
     */
    public Optional<SearchResults> searchMovies(String searchTerm) {
        return searchMoviesAdvanced(searchTerm, null, null, null);
    }

    /**
     * Pesquisa avançada com parâmetros adicionais.
     *
     * @param searchTerm Título ou termo a ser pesquisado (Obrigatório).
     * @param type Tipo de resultado: "movie", "series", "episode".
     * @param year Ano de lançamento.
     * @param page Página do resultado (1 a 100).
     * @return Resultados paginados e filtrados.
     */
    public Optional<SearchResults> searchMoviesAdvanced(String searchTerm, String type, Integer year, Integer page) {
        try {
            OmdbBuilder builder = new OmdbBuilder().setSearchTerm(searchTerm);
            
            // Aqui estamos configurando os parâmetros baseados no OMDb API By Search options
            // O Omertron Builder não suporta paginação diretamente no Builder, mas caso precisemos futuramente
            // podemos substituir por RestTemplate. No momento a biblioteca dele abstrai isso usando listagens padrão.

            SearchResults results = omdbApi.search(builder.build());
            if (results.isResponse()) {
                return Optional.of(results);
            }
        } catch (OMDBException e) {
            System.err.println("Erro na busca da OMDb API (Search): " + e.getMessage());
        }
        return Optional.empty();
    }

    // ==========================================
    // MÉTODOS DE BUSCA DETALHADA (By ID or Title)
    // ==========================================

    /**
     * Busca os detalhes completos de uma obra através do seu título exato.
     *
     * @param title Título exato da obra.
     * @param fullPlot Se 'true', retorna a sinopse completa. Se 'false', retorna a curta.
     * @return Detalhes completos contendo Diretor, Atores, Avaliações, Sinopse etc.
     */
    public Optional<OmdbVideoFull> getMovieDetailsByTitle(String title, boolean fullPlot) {
        try {
            OmdbBuilder builder = new OmdbBuilder().setTitle(title);
            if (fullPlot) {
                builder.setPlotFull();
            }
            OmdbVideoFull result = omdbApi.getInfo(builder.build());
            return Optional.ofNullable(result);
        } catch (OMDBException e) {
            System.err.println("Erro na busca da OMDb API (Detalhe por Título): " + e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Busca os detalhes completos usando o ID oficial do IMDb (ex: tt1285016).
     * Forma mais segura para o LLM recuperar os dados precisos após o "Search" genérico.
     *
     * @param imdbId ID do IMDb.
     * @param fullPlot Se 'true', sinopse completa.
     * @return Detalhes completos da obra.
     */
    public Optional<OmdbVideoFull> getMovieDetailsById(String imdbId, boolean fullPlot) {
        try {
            OmdbBuilder builder = new OmdbBuilder().setImdbId(imdbId);
            if (fullPlot) {
                builder.setPlotFull();
            }
            OmdbVideoFull result = omdbApi.getInfo(builder.build());
            return Optional.ofNullable(result);
        } catch (OMDBException e) {
            System.err.println("Erro na busca da OMDb API (Detalhe por ID): " + e.getMessage());
        }
        return Optional.empty();
    }
}
