$(document).ready(function(){

    // page url
    var pathname =  window.location.pathname;

    $('a.redirect-rewrite').each(function(){
        var url = $(this).attr('href');
        url = url.replace('redirectTo=\./', 'redirectTo=' + pathname);
        $(this).attr('href', url);
    });
});