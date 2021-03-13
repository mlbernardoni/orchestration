var express = require('express');
var http = require('http');
var path = require("path");
var bodyParser = require('body-parser');

var app = express();
var server = http.createServer(app);
var ejs = require('ejs');

var events = require('events');
var eventEmitter = new events.EventEmitter();
const  axios = require('axios');


app.use(bodyParser.urlencoded({extended: false}));
app.use(bodyParser.json());
app.use(express.static(path.join(__dirname,'./')));
//Set EJS as the view engine for Express
app.set('view engine', 'ejs');
app.set('views', [
	  path.join(__dirname, './'),
	]);

app.get('/', function(request,response){
	response.sendFile(path.join(__dirname,'./R2Upload.html'));
});

app.post('/upload', function(request,response){
			//console.log(request);
			//console.log(request.body);
			//console.log("Upload");
			var rootid = "";
		    var file = request.body.file;
		    var auth = request.body.authdropdown;
		    var clear = request.body.cleardropdown ;
			//console.log(file);
			//console.log(auth);
			//console.log(clear);
		  	axios.post( 'http://localhost:8080/R2FileApp/FileAPI.html', 
		  			  {FileName: file, Authenticate: auth, Clearing: clear } )
		  	  .then(function (res) {
		  		  rootid = res.data;
		   		 // console.log(res.data);
		   		  response.writeHead(200,{
		   			  "Content-Type" : "text/html"
		 		  });
		  		  //console.log("File has been submitted");
		  			response.write( rootid);
		  			 response.end();
		   	    	    
		   	  })
		   	  .catch(function (error) {
		   	    console.log(error);
		   	  });	
		 	  });

app.get('/clientcallback', function(request,response){
	//console.log('/clientcallback');
	var rootid = request.url.split('=')[1];
	//console.log(rootid);
	response.writeHead(200, {
	      'Content-Type': 'text/event-stream',
		      'Cache-Control': 'no-cache',
		      'Connection': 'keep-alive',
		      //'Transfer-Encoding': 'identity',
		    	  'Access-Control-Allow-Origin': '*'
		    		  });
	  eventEmitter.once(rootid, function()
	  		  {
	  			  // console.log('Callbacked'); 
	  				response.write("data: " + "apfoafha;lfjoaprioepoiova;lvnasdv=" + rootid + '\n\n'); 			  
	  		  });
	//response.write("OY");
	//response.end();
	});

app.get('/dashboard', (req, res)=> {
	var rootid = req.url.split('=')[1];
	  //console.log(rootid); 
//	var fullUrl = req.protocol + '://' + req.get('host') + req.originalUrl;
	// should move this to node.js R2Lib
	var jsontree = null;
  	axios.post( 'http://localhost:8080/R2FileApp/R2.html', 
			  {root_service: rootid, type:"jsontree" } )
	  .then(function (response) {
		  jsontree  = response.data;
			//console.log(jsontree);
			//myObj =JSON.parse(jsontree); 
			res.render('R2-Dashboard.ejs', {
			    // Pass in rootid as the second argument
			    // to res.render()
				 // jsontree: jsontree
				  jsontree: jsontree
			  });
			});
	  // 'index' must be in views dir
	  return 
	})
	
//server calback
app.post('/callback', function(request,response){
		var rootid = request.url.split('=')[1];
		//console.log(rootid);
		eventEmitter.emit(rootid); // second one is passed on
 		  response.writeHead(200,{
   			  "Content-Type" : "text/html"
 		  });
  			 response.end();
    });



server.listen(46666);
console.log("Server listening on port: 46666");
