$(function () {
  var search_text_el = $('#search-text')
  search_text_el.autocomplete({
    source: search_text_el.data("source"),
    minLength: 3,
    select: function( event, ui ) {
      if (ui.item) {
        window.location = search_text_el.data("target").replace("_PARAM_TIMESTR", ui.item.value);
      }
    }
  });
});
