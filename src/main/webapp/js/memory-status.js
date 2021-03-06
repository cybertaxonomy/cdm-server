
$(document).ready(function(){

  function updateMemoryStatus() {
     $.getJSON(
        "MemoryService.jsp",
        function(data){
          $("#status").html(''); // make empty
          $("#status").html('<div>CPU Cores: ' + data.availableProcessors + '</div>'); // make empty
          $("#status").append(memoryUsageEntry(data.heapMemoryUsage, data.recommendedMinimumHeap, "HeapUsage", "100%", "#F48B65", "#65B1F4"));
          if(data.permGenSpaceUsage){
              $("#status").append(memoryUsageEntry(data.permGenSpaceUsage, data.recommendedMinimumPermGenSpace, "PermGenSpaceUsage", "100%","#F48B65", "#65B1F4"));              
          }
     }); // end JSON


    setTimeout(function(){
        updateMemoryStatus()
        }, 1000);

  } // end updateMemory

  function memoryUsageEntry(memoryUsage, recommended, label, barWidth, cssColorUsed, cssColorMax){
    var mb = 1024 * 1024;
    var gb = mb * 1024;
    
    var max = memoryUsage.max ? memoryUsage.max / mb : -1;
    var used = memoryUsage.used / mb;
    var percent = used * 100 / max;
    var recommendedMB = recommended / mb;
    var html = '';

    html += '<span class="memory-usage">';
    html += label + '(' + used 
    if(recommendedMB > 0){
        html += ' of '  + max+ ' MB, recommended: '  + recommendedMB 
    }
    html += ' MB)&nbsp;';
    html += '<div style="height: 100%; width:' + barWidth + ';background-color:' + cssColorMax + '">';
    html += '<div style="background-color:' + cssColorUsed + '; width:' + percent + '%">&nbsp;</div></div></span>';

    return html;
  }

  updateMemoryStatus()

});