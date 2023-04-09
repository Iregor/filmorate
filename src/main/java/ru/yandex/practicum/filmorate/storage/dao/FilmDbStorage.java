package ru.yandex.practicum.filmorate.storage.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.FilmStorage;

import javax.sql.DataSource;
import java.util.*;

@Repository("filmDb")
@RequiredArgsConstructor
public class FilmDbStorage implements FilmStorage {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    static final RowMapper<Film> filmMapper =
            ((rs, rowNum) -> Film.builder()
                    .id(rs.getLong("FILM_ID"))
                    .name(rs.getString("FILM_NAME"))
                    .description(rs.getString("DESCRIPTION"))
                    .releaseDate(rs.getDate("RELEASE_DATE").toLocalDate())
                    .duration(rs.getInt("DURATION"))
                    .rate(rs.getInt("RATE"))
                    .mpa(new Mpa(rs.getLong("RATING_ID"),
                            rs.getString("RATING_NAME")))
                    .genres(new HashSet<>())
                    .likes(new HashSet<>())
                    .build());

    @Override
    public Collection<Film> findAll() {
        return jdbcTemplate.query(
                "SELECT * FROM FILMS F " +
                        "JOIN RATING MPA ON F.RATING_ID = MPA.RATING_ID " +
                        "LEFT OUTER JOIN (SELECT FILM_ID, COUNT(USER_ID) RATE FROM LIKES " +
                        "GROUP BY FILM_ID) R ON R.FILM_ID = F.FILM_ID " +
                        "ORDER BY F.FILM_ID; ",
                filmMapper);
    }

    @Override
    public Collection<Film> findPopularFilms(int size) {
        return jdbcTemplate.query(
                "SELECT * FROM FILMS F " +
                        "JOIN RATING MPA ON F.RATING_ID = MPA.RATING_ID " +
                        "LEFT OUTER JOIN (SELECT FILM_ID, COUNT(USER_ID) RATE FROM LIKES " +
                        "GROUP BY FILM_ID) R ON R.FILM_ID = F.FILM_ID " +
                        "ORDER BY R.RATE DESC " +
                        "LIMIT :SIZE;",
                new MapSqlParameterSource()
                        .addValue("SIZE", size),
                filmMapper);
    }

    @Override
    public Optional<Film> findById(Long id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    "SELECT * FROM FILMS F " +
                            "JOIN RATING MPA ON F.RATING_ID = MPA.RATING_ID " +
                            "LEFT OUTER JOIN (SELECT FILM_ID, COUNT(USER_ID) RATE FROM LIKES " +
                            "GROUP BY FILM_ID) R ON R.FILM_ID = F.FILM_ID " +
                            "WHERE F.FILM_ID = :FILM_ID;",
                    new MapSqlParameterSource()
                            .addValue("FILM_ID", id),
                    filmMapper));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Film> create(Film film) {
        SimpleJdbcInsert insert = new SimpleJdbcInsert(dataSource)
                .withTableName("FILMS")
                .usingGeneratedKeyColumns("FILM_ID");
        long id = insert
                .executeAndReturnKey(getFilmParams(film))
                .longValue();
        return findById(id);
    }

    @Override
    public Optional<Film> update(Film film) {
        jdbcTemplate.update(
                "UPDATE FILMS " +
                        "SET FILM_NAME = :FILM_NAME, DESCRIPTION = :DESCRIPTION," +
                        "RELEASE_DATE = :RELEASE_DATE, DURATION = :DURATION, " +
                        "RATING_ID = :RATING_ID " +
                        "WHERE FILM_ID = :FILM_ID; ",
                getFilmParams(film));
        return findById(film.getId());
    }

    private MapSqlParameterSource getFilmParams(Film film) {
        return new MapSqlParameterSource()
                .addValue("FILM_ID", film.getId())
                .addValue("FILM_NAME", film.getName())
                .addValue("DESCRIPTION", film.getDescription())
                .addValue("RELEASE_DATE", film.getReleaseDate())
                .addValue("DURATION", film.getDuration())
                .addValue("RATING_ID", film.getMpa().getId());
    }
}
