
var express = require('express');
var http = require('http');
var path = require("path");
var bodyParser = require('body-parser');
var helmet = require('helmet');
var rateLimit = require("express-rate-limit");

var app = express();
var server = http.createServer(app);

const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100 // limit each IP to 100 requests per windowMs
});

var events = require('events');
var eventEmitter = new events.EventEmitter();


app.use(bodyParser.urlencoded({extended: false}));
app.use(express.static(path.join(__dirname,'./public')));
app.use(helmet());
app.use(limiter);


app.get('/', function(request,response){
	response.sendFile(path.join(__dirname,'./public/R2Dashboard.html'));
});

//display
app.post('/display', function(request,response){
	//console.log(request);
    var myid = request.body.rootid;
	  response.writeHead(200,{
			  "Content-Type" : "text/html"
	  });
	 response.write('<link rel = "stylesheet" href="style.css">');
	  response.write( myid);
	  response.end();
	  });

//run
app.post('/run', function(request,response){
	console.log(request.body);
	  var rootid = request.body.rootid;
		console.log("run");
		// first time from form
		if(rootid == "TBD") 
		{
		      var auth = request.body.authdropdown;
		      var clear = request.body.cleardropdown ;
		  	const  axios = require('axios');
		  	axios.post( 'http://localhost:8080/R2FileApp/FileAPI.html', 
		  			  {FileName: 'c:\\input2.csv', Authenticate: auth, Clearing: clear } )
		  	  .then(function (res) {
		  		  rootid = res.data;
		  		  eventEmitter.on(rootid, function()
		  		  {
		  			   console.log('Callbacked'); 
		  			  response.write("<br/><br/>File has completed<br/>Root ID = " + rootid); // need to use the one passed in, because all variables use the emitter thread scope
		  			  response.write("'<br/><form action='/run' method='POST'><input type ='hidden' value = " + rootid + " id='root' name='rootid' required><button type ='submit'>Display</button> </form>'");
		  			  response.end();
		  			  
		  		  });
		  		  console.log(res.data);
		  		  response.writeHead(200,{
		  			  "Content-Type" : "text/html"
				  });
				 response.write('<link rel = "stylesheet" href="style.css">');
				  response.write("File has been submitted  with:<br/>Authentication = "+ auth + "<br/>Clearing = "+ clear + "<br/>Root ID = " + rootid + "<br/><br/>Please wait");
		  		  console.log("File has been submitted");
		  	    	    
		  	  })
		  	  .catch(function (error) {
		  	    console.log(error);
		  	  });
		}
		// on refresh
		else
		{
			//console.log(request);
			//var test = request.header.root;
			//console.log(test);
		    var myid = request.body.rootid;
			  response.writeHead(200,{
					  "Content-Type" : "text/html"
			  });
			 response.write('<link rel = "stylesheet" href="style.css">');
			  response.write( myid);
			  response.end();
		
		}

  	
    });

//calback
app.post('/callback', function(request,response){
		var rootid = request.url.split('=')[1];
		console.log(rootid);
		eventEmitter.emit(rootid); // second one is passed on
    });



server.listen(46666);
console.log("Server listening on port: 46666");

