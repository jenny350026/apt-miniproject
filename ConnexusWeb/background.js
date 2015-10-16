function onClickHandler(info, tab) {
    var sText = info.srcUrl;
    window.open("http://apt-miniproject-1078.appspot.com/chrome_extension?img_url=" + sText, '_blank');
    
    console.log(sText);
}
chrome.contextMenus.onClicked.addListener(onClickHandler);

chrome.runtime.onInstalled.addListener(function (details) {
chrome.contextMenus.create({
type: 'normal',
title: 'Upload to Connexus',
id: 'myContextMenuItem',
contexts: ['image']
}, function () {
console.log('contextMenus are create.');
});
});
