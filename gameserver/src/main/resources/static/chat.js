var chat = {}; // Namespace

(function () {
	"use strict";

	// Constants
	chat.ENTRY_ID = 'entry';
	chat.OUTPUT_ID = 'output';

	// Global variables
	chat.user = null;
	chat.ws = null;

	chat.initPage = function () {
		document.getElementById(chat.ENTRY_ID).focus();

		chat.writeOutput('Initializing GameServer client.', false, "Admin");
		chat.writeOutput('Enter your username:', false, "Admin");
	};

    chat.connect = function() {
        var url = new URL('/ws/' + encodeURI(chat.user), window.location.href);
        url.protocol = url.protocol.replace('http', 'ws');

        chat.ws = new WebSocket(url.href);
        chat.ws.onopen = function(evt) {
            chat.writeOutput('Connection established', false, "Admin");
            chat.writeOutput('Type \'/help\' for a list of commands', false, "Admin");
        };

        chat.ws.onclose = function(evt) {
            chat.writeOutput('Disconnected from server');
        };

        chat.ws.onmessage = function(evt) {
            // KeepAlive messages have no content
            if (evt.data !== '') {
                chat.writeOutput(evt.data, false, "Other"); // ?
            }
            else {
                console.debug('KeepAlive received');
            }
        };

        chat.ws.onerror = function(evt) {
            chat.writeOutput('There was a communications error, check the console for details', false, "Admin");
            console.error("WebSocket Error", evt)
        }
    };

	chat.onEntryKeyPress = function (oCtl, oEvent) {
		if (chat.isEnterKeyPress(oEvent)) {

		    // Capture the current text as a command
            var sEntry = oCtl.value.trim();

			// Reset the text entry for the next command
			oCtl.value = '';

			if (chat.user === null && chat.ws === null) {

                var xhr = new XMLHttpRequest();
                var url = new URL('/user/' + encodeURI(sEntry), window.location.href);
                xhr.open("POST", url, true);

                xhr.onreadystatechange = function () {
                    if (xhr.status === 200) {
                        if (xhr.readyState === 4) {
                            var json = JSON.parse(xhr.responseText);

                            chat.user = {name: json.name, tokens: json.tokens};
                            chat.connect();
                        }
                    } else {
                        chat.writeOutput('Unknown error', false, "Admin");
                    }
                };
                xhr.send();
			}
			else {
				// Process the entry
				if (sEntry !== '') {
    				chat.ws.send(sEntry);
                }
			}

            chat.writeOutput(sEntry, true, "Me");
		}
	};

	chat.isEnterKeyPress = function (oEvent) {
		var keynum;

		if (window.event) { // IE8 and earlier
			keynum = oEvent.keyCode;
		} else if (oEvent.which) { // IE9/Firefox/Chrome/Opera/Safari
			keynum = oEvent.which;
		}

		// Detect ENTER key
		return ('\n' === String.fromCharCode(keynum) || '\r' === String.fromCharCode(keynum));
	};

	chat.writeOutput = function (sOutput, isMe, name) {
        document.getElementById('output').innerHTML += isMe
            ? `
                <div class="wrapper me">
                    <div class="message color-me">
                        <p>${sOutput}</p>
                        <span class="time-left">11:00</span>
                    </div>
                    <div class="circle">
                        ${name.substring(0, 1)}
                    </div>
                </div>
            `
            : `
                <div class="wrapper other">
                    <div class="circle">
                        ${name.substring(0, 1)}
                    </div>
                    <div class="message color-other">
                        <p>${sOutput}</p>
                        <span class="time-right">11:00</span>
                    </div>
                </div>
            `;
        document.getElementById('output').scrollTop = document.getElementById('output').scrollHeight;
	};
}());