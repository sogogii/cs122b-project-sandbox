/**
 * @param target
 */
function getParameterByName(target) {
    // Get request URL
    let url = window.location.href;
    // Encode target parameter name to url encoding
    target = target.replace(/[\[\]]/g, "\\$&");

    // Use regular expression to find matched parameter value
    let regex = new RegExp("[?&]" + target + "(=([^&#]*)|&|#|$)"),
        results = regex.exec(url);
    if (!results) return null;
    if (!results[2]) return '';

    // Return the decoded parameter value
    return decodeURIComponent(results[2].replace(/\+/g, " "));
}

/**
 * Handles the data returned by the API, read the jSonObject and populate data into html elements.
 * @param resultData jsonObject
 */
function handleResult(resultData) {
    $("#previous").prop("disabled", resultData[0]["page"] === 0);
    $("#next").prop("disabled", parseInt(resultData[0]["total_results"]) < parseInt(selectedSort.split(",")[2]));

    $("#page_limit").val(selectedSort.split(",")[2]);

    console.log("handleResult: populating movie list table");
    let movieTableBodyElement = jQuery("#movie_list_table_body");
    movieTableBodyElement.empty();

    for(let i = 1; i < resultData.length; i++) {
        let rowHTML = "";
        rowHTML += "<tr>";
        rowHTML += "<td>" +
            '<a href=../single-pages/single-movie.html?id=' + resultData[i]["movie_id"] + '">' +
            resultData[i]["title"] + '</a></td>';
        rowHTML += "<td>" + resultData[i]["year"] + "</td>";
        rowHTML += "<td>" + resultData[i]["director"] + "</td>";
        rowHTML += "<td>" + resultData[i]["genres"] + "</td>";

        let stars = resultData[i]["stars"];
        let starLinks = stars.map(star =>
            '<a href="../single-pages/single-star.html?id=' + star["star_id"] + '">'
            + star["star_name"] + '</a>');
        rowHTML += "<td>" + starLinks.join(", ") + "</td>";

        rowHTML += "<td>" + resultData[i]["rating"] + "</td>";

        rowHTML += `<td>
            <button class="btn btn-success add-to-cart"
                data-id="${resultData[i]["movie_id"]}"
                data-title="${resultData[i]["title"]}"
                data-price="10.99">
                Add to Cart
            </button>
        </td>`;

        rowHTML += "</tr>";

        movieTableBodyElement.append(rowHTML);
    }

    $(".add-to-cart").off("click").on("click", function () {
        let movieId = $(this).data("id");
        let title = $(this).data("title");
        let price = $(this).data("price");

        $.ajax ({
            type: "POST",
            url: "api/cart",
            data: { movieId, title, price, action: "increase" },
            dataType: "json",
            success: function (response) {
                if (response.status === "success") {
                    alert("✅ Successfully added to cart!");
                } else {
                    alert("❌ Error adding item to cart.");
                }
            },
            error: function () {
                alert("❌ Failed to connect to the server.");
            }
        });
    });
}

function getRequest(page = "") {
    jQuery.ajax({
        dataType: "json",
        method: "GET",
        url: "../api/movie-list?char=" + charId +
            "&search=" + searchTerm +
            "&filter=" + selectedFilter +
            "&order=" + selectedSort +
            "&genre=" + genreFilter +
            "&page=" + page,
        success: (resultData) => handleResult(resultData)
    });
}

let charId = getParameterByName("char");
let searchTerm = getParameterByName("search");
let selectedFilter = getParameterByName("filter");
let genreFilter = getParameterByName("genre");
let selectedSort = "title, DESC, 25";

if (getParameterByName("order") !== null && getParameterByName("order") !== "") {
    selectedSort = getParameterByName("order");
}

$(document).ready(function () {
    getRequest();
    $('#sorting').change(function () {
        selectedSort = $('input[name="sort_input"]:checked').val() + "," +
            $('input[name="order_input"]:checked').val() + "," +
            $('#page_limit').val();
        getRequest("reset");
    });
    $("#previous").click(function () {
        getRequest("previous");
    });
    $("#next").click(function () {
        getRequest("next");
    });
});

$(document).ready(function() {
    // Go to Cart button functionality
    $("#go-to-cart").click(function () {
        window.location.href="../cart.html";
    });
});