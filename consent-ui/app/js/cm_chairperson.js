//DAC CHAIRPERSON PENDING CASES

var jsonData = {};
$.get("json/cm_chairperson_json.json", function(data){
	jsonData = data;
	generatePagination("dul_review", 0);
	generatePagination("access_review", 0);
});

var pages = {
	"dul_review": 0,
	"access_review": 0
}
var PAGINATOR_MAX_ITEMS = 9;
var LIST_ITEMS_MAX_ITEMS = 5;
//
var listItemsTemplate = '<% _.forEach(elections, function(election) { %>';
listItemsTemplate += '<hr class="pvotes-separator">';
listItemsTemplate += '<div class="row pvotes-main-list">';
listItemsTemplate += '<div class="idSample col-lg-6 col-md-6 col-sm-6 col-xs-4 pvotes-list-id"><%= election.sampleId %></div>';

listItemsTemplate += '<a href="<%= election.link %>" class="col-lg-2 col-md-2 col-sm-2 col-xs-2">';
listItemsTemplate += '<div class="voteButton <%= election.status %>">';
listItemsTemplate += '<% if(election.status == "pending"){ %>Vote<% } %>';
listItemsTemplate += '<% if(election.status == "editable"){ %>Edit<% } %>';
listItemsTemplate += '<% if(election.status == "disabled"){ %>---<% } %>';
listItemsTemplate += '</div>';
listItemsTemplate += '</a>';

listItemsTemplate += '<div class="percentageCompleted col-lg-2 col-md-2 col-sm-2 col-xs-3 pvotes-list"><%= election.percentage %>%</div>';

listItemsTemplate += '<a href="<%= election.collectLink %>" class="col-lg-2 col-md-2 col-sm-2 col-xs-3 no-padding">';
listItemsTemplate += '<div class="collectButton <%= election.collect %>">';
listItemsTemplate += '<% if(election.status == "pending"){ %>---<% } %>';
listItemsTemplate += '<% if(election.status == "editable"){ %>Collect Votes<% } %>';
listItemsTemplate += '<% if(election.status == "disabled"){ %>FINAL VOTE<% } %>';
listItemsTemplate += '</div>';
listItemsTemplate += '</a>';

listItemsTemplate += '</div>';
listItemsTemplate += '<% }); %>';


var paginatorTemplate = '<li><a href="#" onclick="generatePagination(\'<%= id %>\', <%= currentPage - 1 %>)">&laquo;</a></li>';
paginatorTemplate += '<% _.forEach(pages, function(page) { %>';
paginatorTemplate += '<li><a href="#" <% if(currentPage == page){ %> class="active-case" <% } %> onclick="generatePagination(\'<%= id %>\', <%= page %>)"><%= page + 1 %></a></li>';
paginatorTemplate += '<% }); %>';
paginatorTemplate += '<li><a href="#" onclick="generatePagination(\'<%= id %>\', <%= currentPage + 1 %>)">&raquo;</a></li>';

function generatePagination(id, page){

	var amountOfElements = jsonData[id].length;
	var isValidPage = ((amountOfElements / LIST_ITEMS_MAX_ITEMS) > page) && (page >= 0);
	
	if(!isValidPage) return;

	pages[id] = page;

	var paginatorId = id  + "_paginator";
	var listItemsId = id  + "_list";
	
	var paginatorHtml = generatePaginator(id, page);
	var listItemsHtml = generateListItemsHtml(jsonData[id], page);
	
	$("#" + paginatorId).html(paginatorHtml);
	$("#" + listItemsId).html(listItemsHtml);
}

function generatePaginator(id, page){

	var delta = Math.floor(PAGINATOR_MAX_ITEMS / 2);
	var numberOfElements = jsonData[id].length;
	var numberOfRanges = Math.floor(numberOfElements / LIST_ITEMS_MAX_ITEMS);
	var floorPosition = page - delta > 0 ? page - delta : 0;
	
	if(floorPosition + PAGINATOR_MAX_ITEMS < numberOfRanges){
		var roofPosition = floorPosition + PAGINATOR_MAX_ITEMS;
	} else {
		var auxPagesMissingFromFloor = floorPosition + PAGINATOR_MAX_ITEMS - numberOfRanges;
		floorPosition = floorPosition - auxPagesMissingFromFloor >= 0 ? floorPosition - auxPagesMissingFromFloor : 0;
		var roofPosition =  numberOfRanges;
	}
	
	var pagesToApply = _.range(floorPosition, roofPosition);

	var compiled = _.template(paginatorTemplate);
	
	return compiled({"id": id,"pages": pagesToApply, "currentPage": page});
}

function generateListItemsHtml(data, page){
	
	var compiled = _.template(listItemsTemplate);
	var floorPosition = page * LIST_ITEMS_MAX_ITEMS;
	var roofPosition = floorPosition + LIST_ITEMS_MAX_ITEMS;
	var reducedJsonData = _.filter(data, function(election, electionPosition){
		return _.inRange(electionPosition, floorPosition, roofPosition);
	});
	
	return compiled({"elections": reducedJsonData});
}
