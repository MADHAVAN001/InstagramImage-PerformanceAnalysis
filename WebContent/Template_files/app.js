      /*
	  // Tell FileDrop we can deal with iframe uploads using this URL:
      var options = {iframe: {url: 'upload.php'}};
      // Attach FileDrop to an area ('zone' is an ID but you can also give a DOM node):
      var zone = new FileDrop('zone', options);

      // Do something when a user chooses or drops a file:
      zone.event('send', function (files) {
        // Depending on browser support files (FileList) might contain multiple items.
        files.each(function (file) {
          // React on successful AJAX upload:
          file.event('done', function (xhr) {
            // 'this' here points to fd.File instance that has triggered the event.
            alert('Done uploading ' + this.name + ', response:\n\n' + xhr.responseText);
          });

          // Send the file:
          file.sendTo('upload.php');
        });
      });

      // React on successful iframe fallback upload (this is separate mechanism
      // from proper AJAX upload hence another handler):
      zone.event('iframeDone', function (xhr) {
        alert('Done uploading via <iframe>, response:\n\n' + xhr.responseText);
      });

      // A bit of sugar - toggling multiple selection:
      fd.addEvent(fd.byID('multiple'), 'change', function (e) {
        zone.multiple(e.currentTarget || e.srcElement.checked);
      });
	  */
var tagURL = "https://www.instagram.com/explore/tags/";
function submitted(likes, tags, thumbnails){
	//$('.likes').text("EXPECTED LIKES: " + likes);
	$('.likes').each(function () {
		$(this).prop('Counter',0).animate({
			Counter: likes
		}, {
			duration: 600,
			easing: 'swing',
			step: function (now) {
				$(this).text(Math.ceil(now));
			}
		});
	});
	$('.tag').remove();
	$('.image-wrapper').remove();
	$('.likes-container').fadeIn();
	for(var i = 0; i < tags.length; i++){
		$('.tags').append($('<a>', {
			href : tagURL + tags[i].tag,
			class : 'tag label label-default'
		}).text('• ' + tags[i].tag).css('background-color', 'rgb(55, 55, ' + (55 + 2 * tags[i].confidence) + ')' ));
	}
	$('.tag').fadeIn();
	for(var i = 0; i < thumbnails.length; i++){
		var thumb = $('<a>', {
			class : 'image-wrapper',
			href : thumbnails[i].link
		});
		thumb.append($('<img>', {
			class : 'image img-thumbnail',
			src : thumbnails[i].link
		}));
		thumb.append($('<div>', {
			class : 'text-overlay'
		}).text('#' + thumbnails[i].tag + " • " + thumbnails[i].likes + " likes"));
		$('.images').append(thumb);
	}
	$('.text-overlay').hover(function() { 
		$(this).animate({
			opacity : '1'
		}); 
	}, function() { 
		$(this).animate({
			opacity : '0'
		}); 
	});
	$('.image').fadeIn();
	
}

function main(result){
	if(!result)
		return;
	likeRatio = 0;
	var map = {};
	string_map = "[";
	string_pam = "[";
	var pam = {};
	for(key in result)
		{
		if(key == "likeRatio")
			likeRatio = result[key];
		if(key == tags)
			{
			value = result[key]; 
			
			for(key1 in value)
				{
				map[key1] = value[key1];
				string_map += "{tag:"+key1+",confidence:"+value[key1]+"}";				
				}
			}
		if(key == "thumbnails")
			{
			value = result[key];
			for(key2 in value)
				{
				pam[key2] = value[key2];

				string_pam += "{link:"+value[key2]+",tag:"+pam[key2]+"}";
				}
			}
			
		}	
	string_map += "]";
	string_pam += "]";
	
	alert(string_map);
	
	
	
		
}
$(document).ready(main);