
$(document).ready(function(){

  function updateInstaceStatus(){
      $.getJSON(
        "BootloaderService.jsp",
        function(data){
          for(var i in data) {
            var dataSourceName = data[i].dataSourceName;
            var status = data[i].status;
            var problems = data[i].problems;
            $('#' + dataSourceName + ' .status').html(status).attr('class', 'status '+ status);
            if(data[i].status == "error" || !data[i].enabled){
             /*out.append("<tr class=\"error-log " + oddOrEven + "\">");
                                                     out.append("<td></td><td  class=\"error\" colspan=\"4\">");
                                                         for( String problem : props.getProblems()){
                                                           out.append("<div>" + problem + "</div>");
                                                         }
                                                     out.append("</td>");
                                                     out.append("</tr>");
              */
              if($('#' + dataSourceName + '-error-log').size() == 0){
                // append new table row
                var oddeven = $('#' + dataSourceName).attr('class').indexOf('even') > -1 ? 'even': 'odd';
                $('#' + dataSourceName).after('<tr id="' + dataSourceName + '-error-log" class="error-log ' + oddeven + '"><td></td><td class="error message" colspan="4"></td></tr>');
              }
              $('#' + dataSourceName + '-error-log .message').html('<h1>x</h1>');
              for(var k in data[i].problems){
                $('#' + dataSourceName + '-error-log .message').appendTo('<div>' + data[i].problems[k] + '</div>');
              }
            }
          }
        });

    setTimeout(function(){
      updateInstaceStatus()
      }, 1000);
  }

  updateInstaceStatus();

});