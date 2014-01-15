
$(document).ready(function(){

  function updateInstanceStatus(){
      $.getJSON(
        "BootloaderService.jsp",
        function(data){
          for(var i in data) {
            var instanceName = data[i].configuration.instanceName;
            var status = data[i].status;
            $('#' + instanceName + ' .status').html(status).attr('class', 'status '+ status);
            if(data[i].status == "error" || !data[i].enabled){
             /*out.append("<tr class=\"error-log " + oddOrEven + "\">");
                                                     out.append("<td></td><td  class=\"error\" colspan=\"4\">");
                                                         for( String problem : props.getProblems()){
                                                           out.append("<div>" + problem + "</div>");
                                                         }
                                                     out.append("</td>");
                                                     out.append("</tr>");
              */
              if($('#' + instanceName + '-messages').size() == 0){
                // append new table row
                var oddeven = $('#' + instanceName).attr('class').indexOf('even') > -1 ? 'even': 'odd';
                $('#' + instanceName).after('<tr id="' + instanceName + '-messages" class="messages ' + oddeven + '"><td></td><td class="error messages" colspan="5"></td></tr>');
              }
//              $('#' + instanceName + '-messages .messages').html('<h1>x</h1>');
              for(var k = 0; k < data[i].messages.length; k++){
                $('#' + instanceName + '-messages .messages').html('<div>' + data[i].messages[k] + '</div>');
              }
            }
          }
        });

    setTimeout(function(){
      updateInstanceStatus()
      }, 1000);
  }

  updateInstanceStatus();

});