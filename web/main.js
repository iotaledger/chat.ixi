const REST_URL = "http://173.249.10.97:4567/";
const REST_URL_GET = REST_URL+"getMessage/";
const REST_URL_SUBMIT = REST_URL+"submitMessage/";
const REST_URL_ADD_CHANNEL = REST_URL+"addChannel/";

const CHANNEL_CODES = {};

const HUES = {
    'A': 14, 'B': 28, 'C': 42, 'D': 56, 'E': 70,
    'F': 84, 'G': 98, 'H': 112, 'I': 126, 'J': 140,
    'K': 154, 'L': 168, 'M': 182, 'N': 196, 'O': 210,
    'P': 214, 'Q': 238, 'R': 252, 'S': 266, 'T': 280,
    'U': 290, 'V': 300, 'W': 312, 'X': 324, 'Y': 336,
    'Z': 348, '9': 0,
};

var channels = {};
var current_channel;

function change_channel(new_channel_name) {

    let code = CHANNEL_CODES[new_channel_name];
    if(channels[code] === undefined)
        channels[code] = [];
    current_channel = code;

    $('#channel_header').text("#"+new_channel_name);
    $('#msgs').html("");

    let hue = HUES[code[0]];
    $('body').css("background-color", " hsl(" + hue + ", 50%, 15%)");

    channels[code].forEach(function (tx) {
        show_message(tx);
    });
}

function new_message(tx) {
    channels[tx['channel']].push(tx);
    show_message(tx);
}

function show_message(tx) {

    let channel = tx['channel'];
    let message = tx['message'];
    let timestamp = tx['timestamp'];
    let user = tx['user'];

    if(channel !== current_channel) { return; }

    const date = new Date(timestamp);
    const time = date.getHours() + ":" + date.getMinutes() + ":" + date.getSeconds();

    const $msg_head = $('<div>').addClass("msg_head")
        .append($('<label>').addClass("username").text(user))
        .append(" at " + time);
    const $msg_body = $('<div>').addClass("msg_body").text(decode(message));
    const $msg = $('<div>').addClass("msg")
        .append($msg_head)
        .append($msg_body);
    $('#msgs').append($msg);
}

function read_message() {
    $.ajax({
        dataType: "json",
        url: REST_URL_GET,
        data: [],
        success: function (tx) {
            new_message(tx);
            read_message();
        },
        error: function (err) { console.log(err) }
    });
}

function submit_message(channel, message) {
    $.ajax({
        url: REST_URL_SUBMIT+channel+"/",
        data: [{"name": "message", "value": encode(message)}],
        success: function () { },
        error: function (err) { console.log(err) }
    });
}

function add_channel(channel_name) {

    CHANNEL_CODES[channel_name] = channel_name.toUpperCase().padEnd(81, "9");

    $('#channels').append($('<div>').addClass('channel').text("#"+channel_name).click(function () {
            change_channel(channel_name);
    }));

    $.ajax({
        url: REST_URL_ADD_CHANNEL+CHANNEL_CODES[channel_name],
        error: function (err) { console.log(err) }
    });
}

function submit() {
    submit_message(current_channel, $('#message').val());
}

function encode(str) {
    return str.replace(/[\u00A0-\u9999<>\&]/gim, function(i) {
        return '&#'+i.charCodeAt(0)+';';
    });
}

function decode(str) {
    return (str+"").replace(/&#\d+;/gm,function(s) {
        return String.fromCharCode(s.match(/\d+/gm)[0]);
    })
}