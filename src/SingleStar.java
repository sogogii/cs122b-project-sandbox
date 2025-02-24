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

@WebServlet(name = "SingleStar", urlPatterns = "/api/single-star")
public class SingleStar extends HttpServlet {
    private static final long serialVersionUID = 2L;
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");

        String id = request.getParameter("id");
        request.getServletContext().log("getting id: " + id);
        PrintWriter out = response.getWriter();

        try (Connection conn = dataSource.getConnection()) {
            HttpSession session = request.getSession();

            String link_search_term = (String) session.getAttribute("search_term");
            String link_search_char = (String) session.getAttribute("search_char");
            String link_genre_filter = (String) session.getAttribute("genre_filter");
            String link_ordering = (String) session.getAttribute("ordering");
            String link_filters = (String) session.getAttribute("filters");
            String link_movie_id = (String) session.getAttribute("movie_id");
            int link_page = (Integer) session.getAttribute("page");

            JsonObject link_json = new JsonObject();
            link_json.addProperty("link_search_term", link_search_term);
            link_json.addProperty("link_search_char", link_search_char);
            link_json.addProperty("link_genre_filter", link_genre_filter);
            link_json.addProperty("link_ordering", link_ordering);
            link_json.addProperty("link_filters", link_filters);
            link_json.addProperty("link_page", link_page);
            link_json.addProperty("link_movie_id", link_movie_id);

            String query = "SELECT * FROM stars WHERE id = ?";
            PreparedStatement statement = conn.prepareStatement(query);
            statement.setString(1, id);
            ResultSet rs = statement.executeQuery();

            JsonArray jsonArray = new JsonArray();
            jsonArray.add(link_json);
            JsonObject jsonObject = new JsonObject();

            rs.next();

            String starId = rs.getString("id");
            String starName = rs.getString("name");
            String starDob = rs.getString("birthYear");

            jsonArray.add(jsonObject);

            query = "SELECT m.title, m.year, m.id " +
                    "FROM movies m, stars s, stars_in_movies sim " +
                    "WHERE sim.starId = s.id AND sim.movieId = m.id AND s.id = ? " +
                    "ORDER BY m.year DESC";
            statement = conn.prepareStatement(query);
            statement.setString(1, starId);
            rs = statement.executeQuery();

            while (rs.next()) {
                jsonObject = new JsonObject();

                String movieTitle = rs.getString("title");
                String movieYear = rs.getString("year");
                String movieId = rs.getString("id");

                jsonObject.addProperty("movie_title", movieTitle);
                jsonObject.addProperty("movie_year", movieYear);
                jsonObject.addProperty("movie_id", movieId);

                jsonArray.add(jsonObject);
            }
            rs.close();
            statement.close();

            out.write(jsonArray.toString());
            conn.close();
            response.setStatus(200);

        } catch (Exception e) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            out.write(jsonObject.toString());

            request.getServletContext().log("Error:", e);
            response.setStatus(500);
        } finally {
            out.close();
        }
    }
}