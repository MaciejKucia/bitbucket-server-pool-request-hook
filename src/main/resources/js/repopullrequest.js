var javafunc = {};

javafunc.wireUpTestButton = function () {
    'use strict';

    var testButton, testHtml = com.maciejkucia.atlasbbplugin.repopullrequest.settings.testButton();
    var helpLink, helpHtml = com.maciejkucia.atlasbbplugin.repopullrequest.settings.helpLink();

    if (AJS.$('button#testButton').length != 0) {
        return;
    }

    testButton = AJS.$('div.dialog-button-panel').prepend(testHtml);
    helpLink = AJS.$('div.dialog-button-panel').prepend(helpHtml);

    //AJS.$('div.dialog-button-panel').prepend('<div class="aui-dialog2-footer-hint">this is a hint</div>');

    AJS.$('#testButton').on('click', function () {
        'use strict';
        var pageState = require('bitbucket/internal/model/page-state');
        var url = AJS.contextPath() + '/plugins/servlet/repopullrequest-weblog/' +
                  pageState.getRepository().getSlug() + '/' + pageState.getProject().getKey();
        window.open(url, '_blank').focus();
    });
};