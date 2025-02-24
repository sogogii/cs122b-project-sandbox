/**
 * Retrieve parameter from request URL, matching by parameter name
 * @param target String
 * @returns {*}
 */
function getParameterByName(target) {
    // Get request URL
    let url = window.location.href;
    // Encode target parameter name to url encoding
    target = target.replace(/[\[\]]/g, "\\$&");

    // Ues regular expression to find matched parameter value
    let regex = new RegExp("[?&]" + target + "(=([^&#]*)|&|#|$)"),
        results = regex.exec(url);
    if (!results) return null;
    if (!results[2]) return '';

    // Return the decoded parameter value
    return decodeURIComponent(results[2].replace(/\+/g, " "));
}

/**
 * Handles the data returned by the API, read the jsonObject and populate data into html elements.
 * @param resultData jsonObject
 */
function handleResult(resultData) {
    console.log("handleResult: populating star info from resultData");

    let movieListLink = jQuery("#movie_list_link");
    movieListLink.empty();

    let linkHMTL = "";
    linkHTML += '<a href="../mainpage/movie-list.html?' +
        'search_term=' + resultData[0]["link_search_term"] + '&' +
        'char=' + resultData[0]["link_search_star"] + '&' +
        'genre=' + resultData[0]["link_genre_filter"] + '&' +
        'order' + resultData[0]["link_ordering"] + '&' +
        'page' + resultData[0]["link_page"] + '&' +
        'filter' + resultData[0]["link_filters"] + '">Back to Movie List</a>';
    movieListLink.append(linkHTML);

    // Populate the star info h3
    // Find the empty h3 body by id "star_info"
    let movieTitle = jQuery("#movie_title");
    let movieInfo = jQuery("#movie_info");

    movieTitle.append("<p>Title: " + resultData[1]["title"] + "</p>");
    movieInfo.append("<p>Release Year: " + resultData[1]["year"] + "</p>" +
        "<p>Director: " + resultData[1]["director"] + "</p>" +
        "<p>Rating: " + resultData[1]["rating"] + "</p>");

    console.log("handleResult: populating movie table from resultData");

    let genreTableBodyElement = jQuery("#genre_table_body");
    genreTableBodyElement.empty();

    for (let i = 0; i < resultData[2]["genre_list"].length; i++) {
        let rowHTML = "";
        rowHTML += "<tr>";
        rowHTML += "<th>" +
            '<a href="../mainpage/movie-list.html?genre=' + resultData[2]["genre_list"][i] + '">' +
            resultData[2]["genre_list"][i] + '</a>' + "</th>";
        rowHTML += "</tr>";
        genreTableBodyElement.append(rowHTML);
    }

    let starsInfo = jQuery("#stars_info");
    starsInfo.empty();

    for (let i = 3; i < resultData.length; i++) {
        let rowHTML = "";
        rowHTML += "<tr>";
        rowHTML += "<th>" +
            '<a href="single-star.html?id=' + resultData[i]['star_id'] + '">' +
            resultData[i]['star_name'] + '</a>' + "</th>";
        rowHTML += "</tr>";
        starsInfo.append(rowHTML);
    }
}

// Get id from URL
let movieId = getParameterByName('id');

// Makes the HTTP GET request and registers on success callback function handleResult
jQuery.ajax({
    dataType: "json",
    method: "GET",
    url: "../api/single-movie?id=" + movieId,
    success: (resultData) => handleResult(resultData)
});