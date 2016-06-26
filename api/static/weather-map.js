$(function () {
  var translate0 = [0, 0], scale0 = 1;  // Initial offset & scale
  var svgs = [];
  d3.selectAll("div.weather-map").each(function () {
    var element = d3.select(this);
    var svg;
    svg = element.style("width", element.attr("data-width"))
                 .style("height", element.attr("data-height"))
                 .append("svg")
                 .attr("width", "100%")
                 .attr("height", "100%")
                 .style("display", "block")
                 .append("g")
                 .call(d3.behavior.zoom().scaleExtent([1, 8]).on("zoom", zoom))
                 .append("g");

    svg.append("image")
       .attr("width", "100%")
       .attr("height", "100%")
       .attr("xlink:href", element.attr("data-image"));

    svg.append("image")
        .attr("width",  "100%")
        .attr("height", "100%")
        .attr("xlink:href", element.attr("data-map-image"));

    svgs.push(svg);

  })
  
  function zoom() {
    svgs.forEach(function (svg) {
      svg.attr("transform", "translate(" + d3.event.translate + ")scale(" + d3.event.scale + ")");
      console.log("translate: " + d3.event.translate + ", scale: " + d3.event.scale);
    });
  }
});
