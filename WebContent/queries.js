

$.ajax({
    url: 'php/upload.php',
    data: $('#file').attr('files'),
    cache: false,
    contentType: 'multipart/form-data',
    processData: false,
    type: 'POST',
    success: function(data){
        alert(data);
    }
});