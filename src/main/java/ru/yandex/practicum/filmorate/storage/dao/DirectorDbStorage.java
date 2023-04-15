package ru.yandex.practicum.filmorate.storage.dao;

import lombok.AllArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.storage.DirectorStorage;

import javax.sql.DataSource;
import java.util.*;

@Repository
@AllArgsConstructor
public class DirectorDbStorage implements DirectorStorage {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    static final RowMapper<Director> directorRowMapper = ((rs, rowNum) -> new Director(
            rs.getLong("DIRECTOR_ID"),
            rs.getString("DIRECTOR_NAME")));

    static final ResultSetExtractor<Map<Long, Set<Director>>> directorsExtractor = rs -> {
        Map<Long, Set<Director>> filmDirectors = new HashMap<>();
        while (rs.next()) {
            filmDirectors.putIfAbsent(rs.getLong("FILM_ID"), new HashSet<>());
            filmDirectors.get(rs.getLong("FILM_ID")).add(new Director(
                    rs.getLong("DIRECTOR_ID"),
                    rs.getString("DIRECTOR_NAME")));
        }
        return filmDirectors;
    };

    @Override
    public Collection<Director> findAll() {
        return jdbcTemplate.query("SELECT * FROM directors", directorRowMapper);
    }

    @Override
    public Optional<Director> findById(Long directorId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    "SELECT * FROM directors WHERE director_id = :DIRECTOR_ID",
                    new MapSqlParameterSource()
                            .addValue("DIRECTOR_ID", directorId),
                    directorRowMapper));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Director> addDirector(Director director) {
        SimpleJdbcInsert insert = new SimpleJdbcInsert(dataSource)
                .withTableName("DIRECTORS")
                .usingGeneratedKeyColumns("DIRECTOR_ID");
        long id = insert
                .executeAndReturnKey(getDirectorParams(director))
                .longValue();
        return findById(id);
    }

    @Override
    public Optional<Director> updateDirector(Director director) {
        jdbcTemplate.update("UPDATE directors SET director_name = :DIRECTOR_NAME " +
                        "WHERE director_id = :DIRECTOR_ID",
                getDirectorParams(director));
        return findById(director.getId());
    }

    @Override
    public void removeDirector(Long directorId) {
        jdbcTemplate.update("DELETE FROM directors WHERE director_id = :DIRECTOR_ID",
                new MapSqlParameterSource()
                        .addValue("DIRECTOR_ID", directorId));
    }

    @Override
    public Collection<Director> findByFilmId(Long filmId) {
        return new HashSet<>(jdbcTemplate.query(
                "SELECT d.director_id, d.director_name " +
                        "FROM directors AS d " +
                        "JOIN film_directors AS fd ON d.director_id = fd.director_id " +
                        "WHERE fd.film_id = :FILM_ID " +
                        "ORDER BY d.director_id",
                new MapSqlParameterSource()
                        .addValue("FILM_ID", filmId),
                directorRowMapper));
    }

    @Override
    public Map<Long, Set<Director>> findByFilms(Set<Long> filmIds) {
        SqlParameterSource ids = new MapSqlParameterSource("IDS", filmIds);
        return jdbcTemplate.query("SELECT * FROM film_directors AS fd " +
                        "JOIN directors AS d ON d.director_id = fd.director_id " +
                        "WHERE film_id IN (:IDS) " +
                        "ORDER BY d.director_id",
                ids, directorsExtractor);
    }

    @Override
    public void addInFilm(Long filmId, Long directorId) {
        jdbcTemplate.update(
                "INSERT INTO FILM_DIRECTORS VALUES (:FILM_ID,:DIRECTOR_ID);",
                new MapSqlParameterSource()
                        .addValue("FILM_ID", filmId)
                        .addValue("DIRECTOR_ID", directorId));
    }

    @Override
    public void removeFromFilm(Long filmId, Long directorId) {
        jdbcTemplate.update(
                "DELETE FROM film_directors " +
                        "WHERE film_id = :FILM_ID AND director_id = :DIRECTOR_ID;",
                new MapSqlParameterSource()
                        .addValue("FILM_ID", filmId)
                        .addValue("DIRECTOR_ID", directorId));
    }

    private MapSqlParameterSource getDirectorParams(Director director) {
        return new MapSqlParameterSource()
                .addValue("DIRECTOR_ID", director.getId())
                .addValue("DIRECTOR_NAME", director.getName());
    }
}
