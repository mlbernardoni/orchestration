
var express = require('express');
var http = require('http');
var path = require("path");
var bodyParser = require('body-parser');
//var helmet = require('helmet');
//var rateLimit = require("express-rate-limit");
//var cors = require('cors')
var app = express();
var server = http.createServer(app);

//const SSE = require('express-sse');
//const sse = new SSE();

//const limiter = rateLimit({
 // windowMs: 15 * 60 * 1000, // 15 minutes
//  max: 100 // limit each IP to 100 requests per windowMs
//});

var events = require('events');
var eventEmitter = new events.EventEmitter();


app.use(bodyParser.urlencoded({extended: false}));
app.use(bodyParser.json());
app.use(express.static(path.join(__dirname,'./')));
//app.use(cors());
//app.use(helmet());
//app.use(limiter);
/*
app.use((req, res, next) => {
	  res.header('Access-Control-Allow-Origin', '*');
	  res.header(
	    'Access-Control-Allow-Headers',
	    'Origin, X-Requested-With, Content-Type, Accept, Authorization'
	  );
	  if (req.method === 'OPTIONS') {
	    res.header('Access-Control-Allow-Methods', 'PUT, POST, PATCH, DELETE, GET');
	    return res.status(200).json({});
	  }
	  next();
	});
*/
app.get('/', function(request,response){
	response.sendFile(path.join(__dirname,'./R2Dashboard.html'));
});

//upload
app.post('/upload', function(request,response){
	//console.log(request);
	console.log(request.body);
	console.log("Upload");
	var rootid = "";
    var file = request.body.file;
    var auth = request.body.authdropdown;
    var clear = request.body.cleardropdown ;
	console.log(file);
	console.log(auth);
	console.log(clear);
  	const  axios = require('axios');
  	axios.post( 'http://localhost:8080/R2FileApp/FileAPI.html', 
  			  {FileName: file, Authenticate: auth, Clearing: clear } )
  	  .then(function (res) {
  		  rootid = res.data;
  		 // console.log(res.data);
  		  response.writeHead(200,{
  			  "Content-Type" : "text/html"
		  });
 		  console.log("File has been submitted");
 			response.write( rootid);
 			 response.end();
  	    	    
  	  })
  	  .catch(function (error) {
  	    console.log(error);
  	  });
	  });

app.get('/clientcallback', function(request,response){
	console.log('/clientcallback');
	var rootid = request.url.split('=')[1];
	console.log(rootid);
	response.writeHead(200, {
	      'Content-Type': 'text/event-stream',
		      'Cache-Control': 'no-cache',
		      'Connection': 'keep-alive',
		      //'Transfer-Encoding': 'identity',
		    	  'Access-Control-Allow-Origin': '*'
		    		  });
	  eventEmitter.once(rootid, function()
	  		  {
	  			   console.log('Callbacked'); 
	  				response.write("data: " + "apfoafha;lfjoaprioepoiova;lvnasdv=" + rootid + '\n\n'); 			  
	  		  });
	//response.write("OY");
	//response.end();
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
	//console.log(request.header);
	  var rootid = request.body.rootid;
		//console.log("run");
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
		  		  eventEmitter.once(rootid, function()
		  		  {
		  			   console.log('Callbacked'); 
		  			  response.write("<br/><br/>File has completed<br/>Root ID = " + rootid); // need to use the one passed in, because all variables use the emitter thread scope
		  			  response.write("'<br/><form action='/run' method='POST'><input type ='hidden' value = " + rootid + " id='root' name='rootid' required><button type ='submit'>Display</button> </form>'");
		  			  response.end();
		  			  
		  		  });
		  		 // console.log(res.data);
		  		  response.writeHead(200,{
		  			  "Content-Type" : "text/html"
				  });
				 response.write('<link rel = "stylesheet" href="style.css">');
				  response.write("File has been submitted  with:<br/>Authentication = "+ auth + "<br/>Clearing = "+ clear + "<br/>Root ID = " + rootid + "<br/><br/>Please wait - do not refresh the browser");
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
		//console.log(rootid);
		eventEmitter.emit(rootid); // second one is passed on
    });



server.listen(46666);
console.log("Server listening on port: 46666");

