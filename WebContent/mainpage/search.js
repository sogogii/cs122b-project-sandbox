function createSearchBar() {
    // Append this to any element that has a search_form id tag. This will be in the main and movie-list page.
    // Creates a table for each of the search inputs. Each row corresponds to a type of input (i.e., title, director, etc.)
    // These rows are identified by _row (i.e., title_row). They have a checkbox that has id of _check (i.e., title_check)
    // If the checkbox is checked, then a new text input element will be appended onto the corresponding row.
    let searchForm = jQuery("#search_form");
    let rowHTML = "";
    rowHTML +=
        "<table id='search_filter_table'>" +
            "<tbody>" +
                "<tr id='title_row'>" +
                    "<td>Title</td>" +
                    "<td><input type='checkbox' id='title_check' checked></td>" +
                "</tr>" +
                "<tr id='director_row'>" +
                    "<td>Director</td>" +
                    "<td><input type='checkbox' id='director_check'></td>" +
                "</tr>" +
                "<tr id='year_row'>" +
                    "<td>Year</td>" +
                    "<td><input type='checkbox' id='year_check'></td>" +
                "</tr>" +
                "<tr id='star_row'>" +
                    "<td>Star Name</td>" +
                    "<td><input type='checkbox' id='star_check'></td>" +
                "</tr>" +
            "</tbody>" +
        "</table>" +
        "<input type='submit' value='Search'>";
    searchForm.append(rowHTML);
}

function updateSearch() {
    // Loops through each of the types of rows using the following array.
    // The purpose of this function is to append the text inputs when the checkbox is checked,
    // and remove it otherwise.
    // Each type has a set of element ids that are used for each of the different important inputs:
    // _check (for the initial checkbox to set up the input field), _search (the text input), and _row
    // to identify where the checkboxes and search input fields should be placed in a page.
    let rows = ["title", "director", "year", "star"]
    for (const key of rows) {
        let check_id = key + '_check';
        let search_id = key + '_search';
        let row_id = key + '_row';
        // This if statement looks for any elements of the type input (like <input>) that have an id of
        // the appropriate _check id that are also checked. This will return an array/list thingy
        // so we have to see if this list is empty or not.
        if (jQuery(`input[id=${check_id}]:checked`).length > 0) {
            console.log(key + ":" + jQuery(`#${search_id}`).length);
            // If the checkbox is checked then we would want to append a new input field, but
            // only if there isn't one already present
            // This is why we have to again check for any already existing elements that have the
            // _search id.
            if (jQuery(`#${search_id}`).length === 0) {
                jQuery(`#${row_id}`).append(`<td><input TYPE='text' id=${search_id}></td>`);
            }
        } else {
            try {
                jQuery(`#${search_id}`).closest('td').remove();
            } catch (error) {

            }
        }
    }
}

// Creates the event driven behavior
$(document).ready(function () {
    // As soon as the document is ready, then the initial table is created
    // and populated with the appropriate rows
    createSearchBar();
    updateSearch();

    // This checks for any change in any element that have an id of search_form,
    // which is what encompasses the table. A change would be like a checking or
    // unchecking of a box.
    $("#search_form").change(function(event) {
        updateSearch();
    });

    // This checks for any submit type events. This is any time an <input type='submit'> is clicked.
    $("#search_form").submit(function(event) {
        event.preventDefault();
        let get_string = "";
        // Here we are trying to build the redirection url with the appropriate search filters (title, director, etc)
        // We do this by again looping through, checking if the appropriate box is checked, and if so then we append a
        // string with the filter and the input text value.
        let filter_rows = ["title_search", "director_search", "star_search", "year_search"];

        for (const key of filter_rows) {
            let check_id = key.replace("_search", "") + "_check";
            if (jQuery(`input[id=${check_id}]:checked`).length > 0) {
                console.log("key: " + key);
                // The key is the element from the filter_rows array. The : serves as a splitter character between the filter
                // and the value for the backend to parse. $(`#${key}`).val() is the value of the text input element.
                get_string += key + ":" + $(`#${key}`).val() + ",";
            }
        }
        console.log("finished get string");
        // Finally, the js redirects the user to the movie-list page with the appropriate parameters in the URL, and the
        // actually GET request gets built in movie-list.js (see movie-list.js for next step in process)
        window.location.href = "movie-list.html?search=" + get_string;
    });
});