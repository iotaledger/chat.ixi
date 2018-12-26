var settings;

var REST_URL;
var REST_URL_GET;
var REST_URL_SUBMIT;
var REST_URL_ADD_CHANNEL;
var REST_URL_ADD_CONTACT;
var REST_URL_GET_ONLINE_USERS;
var REST_URL_INIT;

function reset_settings() {
    settings  = {
        "api": "http://localhost:4567/",
        "hide_strangers": "off",
        "bg_brightness": 30,
        "bg_saturation": 30
    };
}

function put_settings() {
    Object.keys(settings).forEach(function (setting) {
        $("#settings_"+setting).val(settings[setting]);
    });
}

function load_settings() {

    reset_settings();
    Object.keys(settings).forEach(function (setting) {
        let cookie_value = get_cookie("settings_"+setting);
        if(cookie_value !== undefined && cookie_value !== "") {
            settings[setting] = cookie_value;
        }
    });

    put_settings();

    if(!settings['api'].endsWith("/"))
        settings['api'] += "/";
    if(!settings['api'].startsWith("http"))
        settings['api'] = "http://" + settings_api;
    set_rest_urls();
}

function set_rest_urls(rest_url) {
    REST_URL = settings['api'];
    REST_URL_GET = REST_URL+"getMessage/";
    REST_URL_SUBMIT = REST_URL+"submitMessage/";
    REST_URL_ADD_CHANNEL = REST_URL+"addChannel/";
    REST_URL_ADD_CONTACT = REST_URL+"addContact/";
    REST_URL_GET_ONLINE_USERS = REST_URL+"getOnlineUsers";
    REST_URL_INIT = REST_URL+"init";
}

function save_settings() {
    Object.keys(settings).forEach(function (setting) {
        settings[setting] = $("#settings_"+setting).val();
        set_cookie("settings_"+setting, settings[setting]);
    });
    load_settings();
}

function apply_settings() {
    $('#channels').html("");
    $('#online').html("");
    init();
}

function set_cookie(name, value) {
    let date = new Date();
    date.setTime(date.getTime() + 7*24*60*60*1000);
    const time = "expires="+ date.toUTCString();
    document.cookie = name + "=" + value + ";" + time + ";path=/";
}

function get_cookie(name) {
    name = name + "=";
    const decoded_cookie = decodeURIComponent(document.cookie);
    const parts = decoded_cookie.split(';');
    for(let i = 0; i < parts.length; i++) {
        const part = parts[i].trim();
        if (part.indexOf(name) === 0)
            return part.substring(name.length, part.length);
    }
    return "";
}

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
var last_read_of_channel = {};
var current_channel;
var online_users = {};

function change_channel(new_channel_name) {

    current_channel = CHANNEL_CODES[new_channel_name];
    if(current_channel === undefined)
        channels[current_channel] = [];

    $('#channel_header').text("#"+new_channel_name);
    set_channel_bg();
    show_all_messages();
    update_new_msg_counter(current_channel);
}

function set_channel_bg() {
    let hue = HUES[current_channel[0]];
    $('body').css("background-color", " hsl(" + hue + ", "+settings['bg_saturation']+"%, "+settings['bg_brightness']+"%)");
}

function show_all_messages() {
    $('#msgs').html("");
    channels[current_channel].forEach(function (tx) {
        show_message(tx);
    });
    var audio = new Audio('graceful.mp3');
    audio.play();
}

function update_new_msg_counter(channel) {
    const unread_msgs = channels[channel].length - last_read_of_channel[channel];
    $('#channel_' + channel + " .new_msg_counter").text(unread_msgs == 0 ? "" : unread_msgs);
}

function new_message(tx) {
    const channel = tx['channel'];
    if(channels[channel] !== undefined) {
        channels[channel].push(tx);
        show_message(tx);
        update_new_msg_counter(channel);
    }
}

function show_message(tx) {

    const channel = tx['channel'];
    const message = tx['message'];
    const timestamp = tx['timestamp'];
    const username = tx['username'];
    const user_id = tx['user_id'];
    const is_trusted = tx['is_trusted'];
    const is_own = tx['is_own'];

    if(channel !== current_channel) { return; }

    const date = new Date(timestamp);
    const time = ("0"+date.getHours()).slice(-2) + ":" + ("0"+date.getMinutes()).slice(-2) + ":" + ("0"+date.getSeconds()).slice(-2);

    const icon = '<img width=48 height=48 src="data:image/png;base64,' + new Identicon(user_id.padEnd(15, "0"), 48).toString() + '">';

    const $msg_head = $('<div>').addClass("msg_head")
        .append($('<label>').addClass("username").text(username + "@" + user_id.substr(0, 8)))
        .append(" at " + time);
    const $msg_body = $('<div>').addClass("msg_body").text(decode(message));
    const $msg = $('<div>').addClass("msg").addClass("hidden").addClass(is_own ? "own" : is_trusted ? "trusted" : "untrusted")
        .append(icon)
        .append($msg_head)
        .append($msg_body);
    $('#msgs').append($msg);
    setTimeout(function (e) {
        $msg.removeClass("hidden");
    }, 0);

    last_read_of_channel[current_channel] = channels[current_channel].length;

    scrollToBottom();

}

function show_online_users() {
    const $online = $("<div>").attr("id", "online");

    Object.keys(online_users).sort().forEach(function(userid) {
        const online_user = online_users[userid];
        const age = Math.ceil((new Date() - online_user['timestamp']) / 60000);
        const $username = online_user['username'] + "@" + userid.substr(0, 8) + (age <= 5 ? "" : " ("+age+" min)");
        $online.append($("<div>").addClass("user").addClass(age <= 5 ? "online" : "afk").addClass(online_user['is_trusted'] ? "trusted" : "").text($username));
    });
    $('#right_row #online').html($online.html());
}

function read_message() {
    $.ajax({
        dataType: "json",
        url: REST_URL_GET,
        data: [],
        success: function (txs) {
            txs.forEach(function (tx) {
                new_message(tx);
            });
            read_message();
        },
        error: function (err) { console.log(err) }
    });
}

function submit_message(channel, message) {
    const $input_loading = $('#input_loading');
    $input_loading.removeClass("hidden");
    $.ajax({
        url: REST_URL_SUBMIT+channel+"/",
        data: [{"name": "message", "value": encode(message)}],
        success: function (data) { console.log("submitted"); $input_loading.addClass("hidden"); document.getElementById('message').value = "";  },
        error: function (err) { console.log(err); $input_loading.addClass("hidden"); },
    });
}

function add_channel(channel_name) {

    const code = channel_name.toUpperCase().padEnd(81, "9");
    CHANNEL_CODES[channel_name] = code;
    channels[code] = [];
    last_read_of_channel[code] = 0;

    const $channel =
        $('<div>').addClass('channel').attr("id", "channel_"+CHANNEL_CODES[channel_name]).text("#"+channel_name)
            .append($('<label>').addClass('new_msg_counter').text(""))
            .click(function () { change_channel(channel_name); });
    $('#channels').append($channel);

    $.ajax({
        url: REST_URL_ADD_CHANNEL+CHANNEL_CODES[channel_name],
        error: function (err) { console.log(err) }
    });
}

function init() {

    $.ajax({
        url: REST_URL_INIT,
        success: function (data) {
            const initial_channels = ['speculation', 'casual', 'omega', 'qubic', 'announcements'];
            initial_channels.sort().forEach(function(channel) { add_channel(channel); });
            change_channel("announcements");
            read_message();

            setInterval(update_online_users, 30000);
            setTimeout(function () {
                setInterval(submitLifeSign, 300000);
                submitLifeSign();
                setTimeout(update_online_users, 10000);
            }, 10000);
            update_online_users();
        },
        error: function (err) { console.log(err); },
    });
}

function submitLifeSign() {

    $.ajax({
        url: REST_URL_SUBMIT+"LIFESIGN".padEnd(81, "9")+"/",
        data: [{"name": "message", "value":  ""}],
        error: function (err) { console.log(err); },
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

function scrollToBottom() {
    var objDiv = document.getElementById("log");
    objDiv.scrollTop = objDiv.scrollHeight;
}

function addContact() {

    swal({
        title: 'Enter User ID',
        input: 'text'
    }).then(function (text) {

        const user_id = text.value;
        if(!user_id || user_id.length != 8)
            return window.alert("User ID must be 8 trytes long!");

        $.ajax({
            url: REST_URL_ADD_CONTACT + user_id,
            success: function(data) {
                for(var channel in channels) {
                    channels[channel].forEach(function (msg) {
                        if(msg['user_id'] === user_id)
                            msg['is_trusted'] = true;
                    })
                };
                show_all_messages();
            },
            error: function (err) { console.log(err); },
        });

    })

}

function update_online_users() {
    $.ajax({
        dataType: "json",
        url: REST_URL_GET_ONLINE_USERS,
        success: function (data) {
            online_users = data;
            show_online_users();
        },
        error: function (err) { console.log(err); },
    });
}