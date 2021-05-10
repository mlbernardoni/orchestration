 function uploadFile() {
     document.getElementById("status").innerHTML = "Started";
     var iterations = document.getElementById("iterations").value;
     var waitsec = document.getElementById("waitsec").value;
     var file = fileupload.files[0],
         read = new FileReader();
     if (file == undefined) {
         document.getElementById("status").innerHTML = 'Select a file.';
         return;
     }
     read.readAsBinaryString(file);
     read.onloadend = async function () {
         document.getElementById("status").innerHTML = "Uploading File<br>Do not refresh the browser";
         var jsonData = {};
         jsonData["Authenticate"] = document.getElementById("authdropdown").value;
         jsonData["Clearing"] = document.getElementById("cleardropdown").value;
         jsonData["FileName"] = read.result; 
         //jsonData["type"] = "r2s"
        
         //formData.append("file", fileupload.files[0]);
         //formData.append("authdropdown", authdropdown);
         // formData.append("cleardropdown", cleardropdown);
         for (i = 0; i < iterations; i++) {
             console.log("hit"); 
             let response = await fetch("http://localhost:8080/R2FileApp/FileAPI.html", {
                 headers: {
                     'Content-Type': 'application/json'
                 },
                 method: "POST",
                 body: JSON.stringify(jsonData)
             });
             let data = await response.text();
             await sleep(waitsec * 1000);
         }
         document.getElementById("status").innerHTML = "Finished";
         //document.getElementById("p1").innerHTML = "File: " + data +" Complete, check your database";
         //window.location.href = '/R2sDashboard/R2sDashboard.html?id=' + data
     }
     /*	  
     		//Declare an EventSource
     		  const eventSource = new EventSource('/clientcallback');
     		  // Handler for events without an event type specified
     		  eventSource.onmessage = (e) => {
     		    // Do something - event data etc will be in e.data
     */
 }


 function uploadFile_ms() {
     document.getElementById("status").innerHTML = "Started";
     var iterations = document.getElementById("iterations").value;
     var waitsec = document.getElementById("waitsec").value;
     var file = fileupload.files[0],
         read = new FileReader();
     if (file == undefined) {
         document.getElementById("status").innerHTML = 'Select a file.';
         return;
     }
     read.readAsBinaryString(file);
     read.onloadend = async function () {
         document.getElementById("status").innerHTML = "Uploading File<br>Do not refresh the browser";
         var jsonData = {};
         jsonData["Authenticate"] = document.getElementById("authdropdown").value;
         jsonData["Clearing"] = document.getElementById("cleardropdown").value;
         jsonData["FileName"] = read.result;
         //jsonData["type"] = "bspoke"
         //formData.append("file", fileupload.files[0]);
         //formData.append("authdropdown", authdropdown);
         // formData.append("cleardropdown", cleardropdown);
         for (i = 0; i < iterations; i++) {
             let response = await fetch("http://localhost:8080/R2FileApp/FileAPI_ms.html", {
                 headers: {
                     'Content-Type': 'application/json'
                 },
                 method: "POST",
                 body: JSON.stringify(jsonData)
             });
             let data = await response.text();
         }
         //document.getElementById("p1").innerHTML = "File: " + data +" Complete, check your database";
         document.getElementById("status").innerHTML = "Finished";
     }
 }


 function sleep(ms) {
     return new Promise(resolve => setTimeout(resolve, ms));
 }