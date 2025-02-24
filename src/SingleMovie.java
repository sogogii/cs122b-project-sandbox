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

// Declaring a WebServlet called StarsServlet, which maps to url "/api/stars"
@WebServlet(name = "SingleMovie", urlPatterns = "/api/single-movie")
public class SingleMovie extends HttpServlet {
    private static final long serialVersionUID = 3L;
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    /**
     * @see HttpServlet#doGet (HttpServletRequest request, HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");

        String id = request.getParameter("id");
        // Output stream to STDOUT
        request.getServletContext().log("getting id: " + id);
        PrintWriter out = response.getWriter();

        // Get a connection from dataSource and let resource manager close the connection after usage.
        try (Connection conn = dataSource.getConnection()) {
            HttpSession session = request.getSession();

            session.setAttribute("movie_id", id);
            String link_search_term = (String) session.getAttribute("search_term");
            String link_search_char = (String) session.getAttribute("search_char");
            String link_genre_filter = (String) session.getAttribute("genre_filter");
            String link_ordering = (String) session.getAttribute("ordering");
            String link_filters = (String) session.getAttribute("filters");
            int link_page = (Integer) session.getAttribute("page");

            JsonObject link_json = new JsonObject();
            link_json.addProperty("link_search_term", link_search_term);
            link_json.addProperty("link_search_char", link_search_char);
            link_json.addProperty("link_genre_filter", link_genre_filter);
            link_json.addProperty("link_ordering", link_ordering);
            link_json.addProperty("link_filters", link_filters);
            link_json.addProperty("link_page", link_page);

            String query = "SELECT * FROM movies WHERE id = ?";
            PreparedStatement statement = conn.prepareStatement(query);
            statement.setString(1, id);
            ResultSet rs = statement.executeQuery();

            JsonArray jsonArray = new JsonArray();
            jsonArray.add(link_json);
            JsonObject jsonObject = new JsonObject();

            rs.next();

            String title = rs.getString("title");
            String year = rs.getString("year");
            String director = rs.getString("director");

            query = "SELECT * FROM ratings WHERE movieId = ?";
            statement = conn.prepareStatement(query);
            statement.setString(1, id);
            rs = statement.executeQuery();
            rs.next();

            jsonObject.addProperty("title", title);
            jsonObject.addProperty("year", year);
            jsonObject.addProperty("director", director);
            jsonObject.addProperty("rating", rs.getFloat("rating"));

            jsonArray.add(jsonObject);

            // genres
            query = "SELECT g.name " +
                    "FROM genres g, movies m, genres_in_movies gim " +
                    "WHERE g.id = gim.genreId AND gim.movieId = m.id AND m.id = ?";
            statement = conn.prepareStatement(query);
            statement.setString(1, id);
            rs = statement.executeQuery();
            jsonObject = new JsonObject();
            JsonArray genre_list = new JsonArray();

            while (rs.next()) {
                String genre = rs.getString("name");
                genre_list.add(genre);
            }

            // all stars in a movie
            query = "SELECT s.name, s.id " +
                    "FROM stars s, movies m, stars_in_movies sim, " +
                    "(SELECT s.id AS sid, COUNT(*) AS sid_count " +
                    "FROM stars s, stars_in_movies sim " +
                    "WHERE s.id = sim.starId " +
                    "GROUP BY s.id) AS so " +
                    "WHERE s.id = sim.starId AND m.id = sim.movieId AND m.id = ? AND s.id = so.sid " +
                    "ORDER BY so.sid_count DESC";
            statement = conn.prepareStatement(query);
            statement.setString(1, id);
            rs = statement.executeQuery();

            while (rs.next()) {
                jsonObject = new JsonObject();
                jsonObject.addProperty("star_name", rs.getString("name"));
                jsonObject.addProperty("star_id", rs.getString("id"));
                jsonArray.add(jsonObject);
            }

            rs.close();
            statement.close();

            // Write JSON string to output
            out.write(jsonArray.toString());
            conn.close();
            response.setStatus(200);

        } catch (Exception e) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            out.write(jsonObject.toString());

            // Set response status to 500 (Internal Server Error)
            response.setStatus(500);
        } finally {
            out.close();
        }
    }
}



















