var exec = require("cordova/exec");

var DocCrop = function () {};

DocCrop.prototype.cropresult = function (onSuccess, onFail) {
  exec(onSuccess, onFail, "DocCrop", "cropresult");
};

module.exports = new DocCrop();