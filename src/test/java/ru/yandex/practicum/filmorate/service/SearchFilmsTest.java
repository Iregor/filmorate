package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.exception.IncorrectObjectIdException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class SearchFilmsTest {
    private final DirectorService directorService;
    private final FilmService filmService;
    private final JdbcTemplate jdbcTemplate;

    @BeforeEach
    void beforeEach() {
        jdbcTemplate.update("DELETE FROM directors");
        jdbcTemplate.execute("ALTER TABLE directors ALTER COLUMN director_id RESTART WITH 1");
        jdbcTemplate.update("DELETE FROM FILMS ");
        jdbcTemplate.execute("ALTER TABLE FILMS ALTER COLUMN FILM_ID RESTART WITH 1 ");
    }

    @Test
    void getMoviBySearchInTitle() {
        addDataDirectors();
        addDataFilms();
        Collection<Film> collection = filmService.searchFilms("шО", List.of("title"));
        assertThat(collection.size()).isEqualTo(1);
        Film film1 = filmService.findById(1L);
        assertThat(collection).asList().contains(film1);
        assertTrue(film1.getName().toLowerCase().contains("шо"));
    }

    @Test
    void getMoviBySearchInDirector() {
        addDataDirectors();
        addDataFilms();
        Collection<Film> collection = filmService.searchFilms("vano", List.of("director"));
        assertThat(collection.size()).isEqualTo(2);
        Film film1 = filmService.findById(1L);
        Film film2 = filmService.findById(2L);
        assertThat(collection).asList().contains(film1, film2);
        assertTrue(film1.getDirectors().contains(new Director(1L, "Ivanov Ivan")));
    }

    @Test
    void getMovisBySearchInTitleAndDirector() {
        addDataDirectors();
        addDataFilms();
        Collection<Film> collection = filmService.searchFilms("Карт", List.of("title", "director"));
        assertThat(collection.size()).isEqualTo(2);
        Film film1 = filmService.findById(2L);
        Film film2 = filmService.findById(3L);

        assertThat(collection).asList().contains(film1, film2);
        assertTrue(film1.getName().toLowerCase().contains("карт"));
        assertTrue(film2.getDirectors().contains(new Director(3L, "Василий Картапов")));
    }

    @Test
    void noFoundBySearch() throws IncorrectObjectIdException {
        addDataDirectors();
        addDataFilms();
        Collection<Film> collection = filmService.searchFilms("какой-то текст", List.of("title", "director"));
        assertTrue(collection.size() == 0);
    }

    private void addDataDirectors() {
        jdbcTemplate.update("INSERT INTO DIRECTORS (director_id, director_name) " +
                "VALUES (1, 'Ivanov Ivan'), (2, 'Petrov Petr'), (3, 'Василий Картапов')");
    }

    private void addDataFilms() {
        filmService.create(Film.builder()
                .name("Побег из Шоушенка")
                .duration(142)
                .releaseDate(LocalDate.of(1994, 9, 24))
                .mpa(new Mpa(2L, "PG"))
                .genres(new HashSet<>(List.of(new Genre(2L, "Драма"))))
                .description("Бухгалтер Энди Дюфрейн обвинён в убийстве собственной жены")
                .directors(new HashSet<>(List.of(new Director(1L, "Ivanov Ivan")))).build());

        filmService.create(Film.builder()
                .name("Карты, деньги, два ствола")
                .duration(142)
                .releaseDate(LocalDate.of(1998, 9, 24))
                .mpa(new Mpa(2L, "PG"))
                .genres(new HashSet<>(List.of(new Genre(1L, "Комедия"))))
                .description("Стейтем не бьет морды")
                .directors(new HashSet<>(List.of(new Director(1L, "Ivanov Ivan")))).build());

        filmService.create(Film.builder()
                .name("Shadow of Dragon")
                .duration(100)
                .releaseDate(LocalDate.of(2007, 4, 22))
                .mpa(new Mpa(2L, "PG"))
                .genres(new HashSet<>(List.of(new Genre(1L, "Ужасы"))))
                .description("Бойся дракона")
                .directors(new HashSet<>(List.of(new Director(3L, "Василий Картапов")))).build());
    }
}