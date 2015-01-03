var _dialog_html = 
'<div class="modal" id="dialog" tabindex="-1" role="dialog" aria-labelledby="dialog_label">' +
'    <div class="modal-dialog">' +
'        <div class="modal-content">' +
'            <div class="modal-header">' +
'                <h4 class="modal-title" id="dialog_head"></h4>' +
'            </div>' +
'            <p class="modal-body" id="dialog_body"></p>' +
'            <div class="modal-footer" id="dialog_footer">' +
'                <button type="button" class="btn btn-danger" id="dialog_close">Close</button>' +
'            </div>' +
'        </div>' +
'    </div>' +
'</div>';

var _dialog_loaded = false;

$.dialog = function(title, text, cancellable) {
    var dialog;
    if (!_dialog_loaded) {
        dialog = $($.parseHTML(_dialog_html));
        dialog.hide();
        dialog.find("#dialog_close").click(function() { $.dialog_hide(); });
        $("body").append(dialog);
        _dialog_loaded = true;
    } else {
        dialog = $("#dialog");
    }
    dialog.find("#dialog_head").text(title);
    var body = dialog.find("#dialog_body");
    if (text) {
        body.empty();
        if (typeof text === "string") {
            body.text(text);
        } else {
            body.append(text);
        }
        body.show();
    } else {
        body.hide();
    }
    if (cancellable || cancellable === undefined) {
        dialog.find("#dialog_footer").show();
    } else {
        dialog.find("#dialog_footer").hide();
    }
    dialog.slideDown();
}

$.dialog_hide = function() {
    $("#dialog").slideUp();
};
