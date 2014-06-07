// Define a WidgetManager class
function WidgetManager(url){
   this.url = url;
   this.socket;
}

WidgetManager.prototype.startWebSocket = function(callbacks) {
	
	var host = this.url;
	
	if (window.console) {
		console.log("Connecting to "+host+" ...");
	}
	
	if (window.MozWebSocket) { // for Firefox
		window.WebSocket=window.MozWebSocket;
	}
	
	if (!window.WebSocket) {
		if (window.console) {
			console.log("This browser doesn't support websockets.");
		}
		alert("ERROR : Sorry, this application only works on browsers with WebSocket technology.");
		return false;
	} else {
		this.socket = new WebSocket(host);
		
		this.socket.onopen = function() { if (window.console) console.log("Socket opened."); }
		this.socket.onclose = function() { if (window.console) console.log("Socket closed."); }
		this.socket.onerror = function() { if (window.console) console.log("Socket error."); }
		
		// msg has form <function_name:argument> or <function_name>
		this.socket.onmessage = function(msgEvent) {
			var msg = msgEvent.data;
			
			console.log("msg = "+msg);
			index = msg.indexOf(":");
			if (index < 0) { // function has no argument
				callbacks[msg]();
			} else {
				func = msg.split(":")[0];
				arg = msg.substring(index+1);
				console.log("CALLBACKS : "+callbacks[func]);
				callbacks[func](arg);
			}
		}
	}
	return true;
}

WidgetManager.prototype.serverExec = function(func, arg) {
	if (func && !arg) {
		this.sendMessage(func);
	} else {
		this.sendMessage(func+":"+arg);
	}
}

WidgetManager.prototype.sendMessage = function(msg) {
	if(typeof msg=='object') {
		msg = JSON.stringify(msg); // converting any data to string
	}
	
	if(this.socket) {
		if (window.console) console.log("Sending to server -> "+msg);
		this.socket.send(msg);
	} else {
		console.error("Socket undefined while trying to send.");
	}
}