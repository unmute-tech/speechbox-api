// Defined dynamically
// var markers = [
//     [0,0,"Box 1"],
//     [10,10,"Box 2"],
// ];
var map = L.map('map')
L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
}).addTo(map);

//Loop through the markers array
var markerArray = []
for (var i=0; i<markers.length; i++) {
    var lon = markers[i][0];
    var lat = markers[i][1];
    var popupText = markers[i][2];

    var markerLocation = new L.LatLng(lat, lon);
    var marker = new L.marker(markerLocation);
    markerArray.push(marker)
    marker.addTo(map).bindPopup(popupText);
}

var group = L.featureGroup(markerArray);
map.fitBounds(group.getBounds());
