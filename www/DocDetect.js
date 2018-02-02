var exec = require("cordova/exec");

var DocDetect = function () {};

DocDetect.prototype.cropresult = function (onSuccess, onFail) {
  exec(onSuccess, onFail, "DocDetect", "cropresult");
};

module.exports = new DocDetect();