var javafunc = {};

javafunc.wireUpTestButton = function () {
    'use strict';
    var testButton,
        testHtml = com.maciejkucia.repopullrequest.hook.PullRequestHook.testButton();

    if (AJS.$('button#testButton').length != 0) {
        return;
    }

    testButton = AJS.$('div.dialog-button-panel').prepend(testHtml);

    testButton.on('click', function () {
        'use strict';
        alert('Message');
    });
};