import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

@WebServlet(name = "MovieList", urlPatterns = "/api/movie-list")
public class MovieList extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");

        PrintWriter out = response.getWriter();

        try (Connection conn = dataSource.getConnection()) {
            String search_term = "";
            String filter_array = "";
            StringBuilder search_and_filter = new StringBuilder();

            HttpSession session = request.getSession();
            int current_page = 0;

            if ((Integer) session.getAttribute("page") != null) {
                current_page = (Integer) session.getAttribute("page");
            }
            if (request.getParameter("page").equals("previous")) {
                current_page--;
            } else if (request.getParameter("page").equals("next")) {
                current_page++;
            } else if (request.getParameter("page").equals("reset")) {
                current_page = 0;
            }
            session.setAttribute("page", current_page);

            int set_string = 0;
            ArrayList<String> searches = new ArrayList<String>();
            boolean year = false;

            if (request.getParameter("search") != null && !request.getParameter("search").equals("null")) {
                search_term = request.getParameter("search");
                String[] filters = search_term.split(",");

                for (int i = 0; i < filters.length; i++) {
                    String filter = filters[i].split(":")[0];
                    System.out.println("filter: " + filter);
                    searches.add(filters[i].split(":")[1]);
                    System.out.println("search term: " + filters[i].split(":")[1]);

                    switch (filter) {
                        case "title_search":
                            search_and_filter.append("AND m.title LIKE ? ");
                            set_string++;
                            break;
                        case "director_search":
                            search_and_filter.append("AND m.director LIKE ? ");
                            set_string++;
                            break;
                        case "year_search":
                            search_and_filter.append("AND m.year LIKE ? ");
                            year = true;
                            break;
                        case "star_search":
                            search_and_filter.append("AND s.name LIKE ? ");
                            break;
                        default:
                            break;
                    }
                }
            }

            String search_char = "";
            String search_char_query = "";

            if (request.getParameter("char") != null && !request.getParameter("char").equals("null")) {
                search_char = request.getParameter("char");
                if (search_char.equals("*")) {
                    search_char_query += "AND m.title NOT REGEXP '[0123456789abcdefghijklmnopqrstuvwxyz]' ";
                } else {
                    search_char_query += "AND m.title LIKE '" + search_char + "%'";
                }
            }

            // String ordering = "ORDER BY r.rating DESC LIMIT 20";
            String ordering = "";
            String ordering_array = request.getParameter("order");
            String[] orderings = ordering_array.split(",");

            if (orderings[0].equals("rating")) {
                ordering += "ORDER BY r.rating DESC, m.title DESC";
            } else {
                ordering += "ORDER BY m.title DESC, r.rating DESC";
            }
            if (orderings[1].equals("ASC")) {
                ordering = ordering.replace("DESC", "ASC");
            }

            ordering += "LIMIT " + orderings[2] + " OFFSET " + current_page * Integer.parseInt(orderings[2]);
            String genre = "";
            String genre_filter = "";
            if (!request.getParameter("genre").equals("null") && !request.getParameter("genre").isEmpty()) {
                genre = request.getParameter("genre");
                genre_filter += "AND g.name = '" + request.getParameter("genre") + "' ";
            }

            session.setAttribute("search_term", search_term);
            session.setAttribute("search_char", search_char);
            session.setAttribute("genre_filter", genre);
            session.setAttribute("ordering", ordering_array);
            session.setAttribute("filters", filter_array);

            String query =
                    "SELECT DISTINCT m.id, m.title, m.year, m.director, r.rating " +
                            "FROM movies m, ratings r, stars s, stars_in_movies sim, genres g, genres_in_movies gim " +
                            "WHERE m.id = r.movieId AND m.id = sim.movieId AND sim.starId = s.id AND g.id = gim.genreId AND m.id = gim.movieId " +
                            search_and_filter + search_char_query + genre_filter + ordering;

            PreparedStatement statement = conn.prepareStatement(query);
            System.out.println(query);

            int i;
            for (i = 1; i < set_string; i++) {
                System.out.println("set_string: " + i + " " + searches.get(i - 1));
                statement.setString(i, "%" + searches.get(i - 1) + "%");
            }
            if (year) {
                statement.setString(i, searches.get(i - 1));
            }

            System.out.println("set string");
            ResultSet rs = statement.executeQuery();
            System.out.println("finished query");
            JsonArray jsonArray = new JsonArray();
            JsonObject cur_page = new JsonObject();
            cur_page.addProperty("page", current_page);
            jsonArray.add(cur_page);
            int total_results = 0;

            String movieId = null;
            while (rs.next()) {
                total_results++;
                JsonObject jsonObject = new JsonObject();
                movieId = rs.getString("id");

                jsonObject.addProperty("movie_id", movieId);
                jsonObject.addProperty("title", rs.getString("title"));
                jsonObject.addProperty("year", rs.getString("year"));
                jsonObject.addProperty("director", rs.getString("director"));
                jsonObject.addProperty("rating", rs.getString("rating"));

                // Genres
                String genreQuery =
                        "SELECT g.name " +
                                "FROM genres g, genres_in_movies gim " +
                                "WHERE g.id = gim.genreId AND gim.movieId = ?";
                PreparedStatement genreStatement = conn.prepareStatement(genreQuery);
                genreStatement.setString(1, movieId);
                ResultSet genreRs = genreStatement.executeQuery();
                JsonArray genres = new JsonArray();

                while (genreRs.next()) {
                    genres.add(genreRs.getString("name"));
                }

                jsonArray.add("genres");
                genreRs.close();
                genreStatement.close();

                // Stars
                String starQuery =
                        "SELECT s.id, s.name " +
                                "FROM stars s, stars_in_movies sim " +
                                "WHERE s.id = sim.starId AND sim.movieId = ?";
                PreparedStatement starStatement = conn.prepareStatement(starQuery);
                starStatement.setString(1, movieId);
                ResultSet starRs = starStatement.executeQuery();
                JsonArray stars = new JsonArray();

                while (starRs.next()) {
                    JsonObject starObject = new JsonObject();
                    starObject.addProperty("star_id", starRs.getString("id"));
                    starObject.addProperty("star_name", starRs.getString("name"));
                    stars.add(starObject);
                }
                jsonArray.add("stars");
                starRs.close();
                starStatement.close();

                jsonArray.add(jsonObject);
            }

            jsonArray.get(0).getAsJsonObject().addProperty("total_results", total_results);
            System.out.println("total results: " + total_results);
            rs.close();
            statement.close();

            out.write(jsonArray.toString());
            response.setStatus(200);

        } catch (Exception e) {
            // getServletContext().log("Error in movie listServlet: ", e);

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());

            response.getWriter().write(jsonObject.toString());
        } finally {
            out.close();
        }
    }
}