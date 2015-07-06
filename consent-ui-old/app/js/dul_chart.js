//COLLECTING VOTE RESULTS CHART

google.load("visualization", "1", {packages:["corechart"]});
google.setOnLoadCallback(drawChart);
function drawChart() {

    var data = google.visualization.arrayToDataTable([
        ['Results', 'Votes'],
        ['Yes', 3],
        ['No', 2],
        ['Pending', 1]
    ]);

    var options = {
        pieHole: 0.4,
        pieSliceTextStyle: {
            color: 'white',
            fontSize: 16
        },
        pieSliceText: 'none',
        pieSliceBorderColor: 'transparent',
        backgroundColor: 'transparent',
        chartArea: {
            left: 0,
            top: 10,
            right: 0,
            bottom: 10,
            width:'100%',
            height:'85%'
        },
        height: 138,
        slices: {
            0: { color: '#C16B0C' },
            1: { color: '#777777' },
            2: { color: '#c9c9c9' }
        },
        legend: {
            position: 'right',
            textStyle: {
                color: '#777777',
                bold: true,
                fontName: 'Roboto',
                fontSize: 14
            },
            alignment: 'start'
        },
        tooltip: {
            textStyle: {
                color: 'black',
                fontSize: 14
            }
        }
    };

    var chart = new google.visualization.PieChart(document.getElementById('dulResultsChart'));
    chart.draw(data, options);
}
