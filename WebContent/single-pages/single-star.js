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
    console.log("handleResult: populating star info from resultDAta");

    let movieListLink = jQuery("#movie_list_link");
    movieListLink.empty();

    let linkHTML = "";
    linkHTML += '<a href = "../mainpage/movie-list.html?' +
        'search_term=' + resultData[0]["link_search_term"] + '&' +
        'char=' + resultData[0]["link_search_char"] + '&' +
        'genre=' + resultData[0]["link_genre_filter"] + '&' +
        'order=' + resultData[0]["link_ordering"] + '&' +
        'page=' + resultData[0]["link_page"] + '&' +
        'filter=' + resultData[0]["link_filters"] + '">Back to Movie List</a>';
    movieListLink.append(linkHTML);

    // populate the star info h3
    // find the empty h3 body by id "star_info"
    let starInfoElement = jQuery("#star_info");

    // append two html <p> created to the h3 body, which will refresh the page
    starInfoElement.append("<p>Star Name: " + resultData[1]["star_name"] + "</p>" +
        "<p>Date Of Birth: " + resultData[1]["star_dob"] + "</p>");

    console.log("handleResult: populating movie table from resultData");

    // populate the star table
    // find the empty table body by id "movie_table_body"
    let movieTableBodyElement = jQuery("#movie_table_body");

    // Concatenate the html tags with resultData jsonObject to create table rows
    for (let i = 2; i < resultData.length; i++) {
        let rowHTML = "";
        rowHTML += "<tr>";
        rowHTML += "<th>" +
            '<a href="single-movie.html?id=' +
            resultData[i]["movie_id"] + '">' + resultData[i]["movie_title"] + '</a>' + "</th>";
        rowHTML += "<th>" + resultData[i]["movie_year"] + "</th>";
        rowHTML += "</tr>";

        movieTableBodyElement.append(rowHTML);
    }
}

// Get id from URL
let starId = getParameterByName('id');

// Makes the HTTP GET request and registers on success callback function handleResult
jQuery.ajax({
    dataType: "json",
    method: "GET",
    url: "../api/single-star?id=" + starId,
    success: (resultData) => handleResult(resultData)
});