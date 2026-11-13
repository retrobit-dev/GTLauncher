# QR code generator

`qrcode.js` is a Browserify bundle of npm `qrcode` 1.5.4 and `dijkstrajs` 1.0.3, both MIT licensed (see accompanying license files). It is bundled locally so exporting world data does not send it to a third-party service.

To rebuild: install those packages and Browserify in a temporary directory, create an entry containing `window.WorldQRCode = require('qrcode');`, then run Browserify on the entry to produce `qrcode.js`.
