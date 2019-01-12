window.onerror = function(err) {
    swal("Woops!", "Looks like there was an error. Check your browser console.", "error");
};

var settings;
var last_life_sign = 0;
var initialized = false;

var REST_URL;
var REST_URL_GET;
var REST_URL_SUBMIT;
var REST_URL_ADD_CHANNEL;
var REST_URL_REMOVE_CHANNEL;
var REST_URL_ADD_CONTACT;
var REST_URL_REMOVE_CONTACT;
var REST_URL_GET_ONLINE_USERS;
var REST_URL_INIT;
set_rest_urls();

const icons = {};
const audio = new Audio('sound.ogg');


setInterval(update_online_users, 10000);
setInterval(submit_life_sign, 10000);

function reset_settings() {
    settings  = {
        "hide_strangers": "off",
        "bg_brightness": 30,
        "bg_saturation": 30,
        "history_size": 100,
        "password": ""
    };
}
function load_settings() {

    reset_settings();
    Object.keys(settings).forEach(function (setting) {
        let cookie_value = get_cookie("settings_"+setting);
        if(cookie_value !== undefined && cookie_value !== "") {
            settings[setting] = cookie_value;
        }
    });

    settings['history_size'] = parseInt(settings['history_size']);
    //set_rest_urls();
}

function set_rest_urls() {
    REST_URL = window.location.protocol + "//" + window.location.host + "/";
    REST_URL_GET = REST_URL+"getMessage/";
    REST_URL_SUBMIT = REST_URL+"submitMessage/";
    REST_URL_ADD_CHANNEL = REST_URL+"addChannel/";
    REST_URL_REMOVE_CHANNEL = REST_URL+"removeChannel/";
    REST_URL_ADD_CONTACT = REST_URL+"addContact/";
    REST_URL_REMOVE_CONTACT = REST_URL+"removeContact/";
    REST_URL_GET_ONLINE_USERS = REST_URL+"getOnlineUsers";
    REST_URL_INIT = REST_URL+"init";
}

function put_settings() {
    Object.keys(settings).forEach(function (setting) {
        $("#settings_"+setting).val(settings[setting]);
    });
    $('#settings_hide_strangers').prop("checked", settings['hide_strangers'] === 'true');
}

function pull_settings() {
    Object.keys(settings).forEach(function (setting) {
        settings[setting] = $("#settings_"+setting).val();
    });
    settings['history_size'] = parseInt(settings['history_size']);
    settings['hide_strangers'] = $("#settings_hide_strangers").is(':checked');
}


function save_settings() {

    if(/Google Inc/.test(navigator.vendor) && /Chrome/.test(navigator.userAgent) && window.location.protocol === 'file:') {
        swal(':\'(', "Chrome does not support cookies on local pages. Settings will be lost on refresh.", 'warning');
    }

    Object.keys(settings).forEach(function (setting) {
        set_cookie("settings_"+setting, settings[setting]);
    });
    set_cookie("settings_hide_strangers", settings['hide_strangers']);
}

function apply_settings() {
    $('#channels').html("");
    $('#online').html("");
    $('#msgs').html("");
    init();
}

function set_cookie(name, value) {
    let date = new Date();
    date.setTime(date.getTime() + 7*24*60*60*1000);
    const time = "expires="+ date.toUTCString();
    document.cookie = name + "=" + value + ";" + time + ";";
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

    $('#msgs .msg').remove();
    current_channel = CHANNEL_CODES[new_channel_name];

    if(channels[current_channel] === undefined)
        channels[current_channel] = [];

    $('#channel_header').text("#"+new_channel_name);
    change_colors();
    show_all_messages();
    update_new_msg_counter(current_channel);
}

function change_colors() {
    let hue = HUES[current_channel[0]];
    $('body').css("background-color", " hsl(" + hue + ", "+settings['bg_saturation']+"%, "+settings['bg_brightness']+"%)");

    let chue = (hue+90)%360;
    if(chue > 150 && chue < 250) // blue is hard to see
        chue += 100;

    $("#style").text("a { color: hsl(" + chue + ",  100%, 50%) }");
}

function show_all_messages() {
    channels[current_channel].forEach(function (tx) {
        show_message(tx);
    });
}

function update_new_msg_counter(channel) {
    const unread_msgs = channels[channel].length - last_read_of_channel[channel];
    $('#channel_' + channel + " .new_msg_counter").text(unread_msgs == 0 ? "" : unread_msgs);
}

function new_message(tx) {
    const channel = tx['channel'];
    if(channels[channel] !== undefined) {
        if(channels[channel].length >= settings['history_size'])
            channels[channel].shift();
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

    if(settings['hide_strangers'] === 'true' && !is_trusted)
        return;

    if(channel !== current_channel) { return; }

    const date = new Date(timestamp);
    const time = ("0"+date.getHours()).slice(-2) + ":" + ("0"+date.getMinutes()).slice(-2) + ":" + ("0"+date.getSeconds()).slice(-2);

    let icon = icons[user_id];
    if(!icon) {
        icon = '<img width=48 height=48 src="data:image/png;base64,' + new Identicon(trytes_to_hex_with_loss(user_id+user_id), 48).toString() + '">';
        icons[user_id] = icon;
    }

    const $msg_head = $('<div>').addClass("msg_head")
        .append($('<label>').addClass("username")
            .click(function () {
                copy("@"+user_id, "@"+user_id);
            })
        .text(username + "@" + user_id.substr(0, 8)))
        .append(" at " + time);


    const urlRegex = /((https?:\/\/|www.)(www.)?[^ "']*)/gim;

    const msg = emoji.replace_colons(decode(message).split("&").join("&amp;").split("<").join("&lt;").split(">").join("&gt;"))
        .replace(/[\n]/g, "<br/>")
        .replace(urlRegex, '<a href="$1" target="_blank">$1</a>')
        .replace(/(\/emoji-data\/)/g, "https://raw.githubusercontent.com/iamcal/emoji-data/a97b2d2efa64535d6300660eb2cd15ecb584e79e/");

    const $msg_body = $('<div>').addClass("msg_body").html(msg);
    const $msg = $('<div>').addClass("msg").addClass("hidden").addClass(is_own ? "own" : is_trusted ? "trusted" : "untrusted")
        .append(icon)
        .append($msg_head)
        .append($msg_body);


    $('#msgs').append($msg);

    console.log("showed message, now: " + $('#msgs .msg').length);

    var log = document.getElementById("log");
    log.scrollTop = log.scrollHeight;

    setTimeout(function (e) {
        $msg.removeClass("hidden");
    }, 0);


    const $msgs = $('#msgs .msg');
    if($msgs.length > settings['history_size']) {
        //alert($msgs.length)
        $msgs.first().remove();
    }

    last_read_of_channel[current_channel] = channels[current_channel].length;
}

function trytes_to_hex_with_loss(trytes) {
    const replacer = {"G": "0", "H": "1", "I": "2", "J": "3", "K": "4", "L": "5", "M": "6", "N": "7", "O": "8", "P": "9",
    "Q": "0", "R": "2", "S": "4", "T": "6", "U": "8", "V": "0", "W": "A", "X": "B", "Y": "D", "Z": "F"}

    Object.keys(replacer).forEach(function (tryte) {
        trytes = trytes.split(tryte).join(replacer[tryte]);
    });

    return trytes.toLowerCase();
}

function show_online_users() {
    const $list = $("<div>");

    Object.keys(online_users).sort().forEach(function(userid) {
        const online_user = online_users[userid];
        const age = Math.ceil((new Date() - online_user['timestamp']) / 60000);
        const $username = online_user['username'] + "@" + userid + (age <= 5 ? "" : " ("+age+" min)");
        const $user = $("<div>").addClass("user").addClass(age <= 5 ? "online" : "afk").addClass(online_user['is_trusted'] ? "trusted" : "")
            .text($username).click(function () { copy("@"+userid, "@"+userid); });
        $list.append($user);
    });
    const $online = $('#right_row #online');
    $online.html("").append($list);
}

function submit_message(channel, message) {
    const $input_loading = $('#input_loading');
    $input_loading.removeClass("hidden");
    $.ajax({
        url: REST_URL_SUBMIT+channel+"/",
        method: 'POST',
        data: [{"name": "message", "value": encode(message)}, {'name': 'password', 'value': settings['password']}],
        success: function (data) { $input_loading.addClass("hidden"); document.getElementById('message').value = "";  },
        error: function (err) { console.log(err); $input_loading.addClass("hidden"); },
    });
}

function add_channel(channel_name) {

    add_channel_internally(channel_name);

    $.ajax({
        url: REST_URL_ADD_CHANNEL,
        method: 'POST',
        data: [{"name": "name", "value": channel_name}, {'name': 'password', 'value': settings['password']}],
        error: function (err) { console.log(err) }
    });
}

function remove_channel(channel_name) {

    let code = derive_channel_address_from_name(channel_name);

    Object.keys(CHANNEL_CODES).forEach(function (cn) {
        if(CHANNEL_CODES[cn] === code) {
            channels[cn] = undefined;
            $('#channel_'+CHANNEL_CODES[cn]).remove();
        }
    });

    $.ajax({
        url: REST_URL_REMOVE_CHANNEL,
        method: 'POST',
        data: [{"name": "name", "value": channel_name}, {'name': 'password', 'value': settings['password']}],
        error: function (err) { console.log(err) }
    });
}

function add_channel_internally(channel_name) {

    channel_name = channel_name.replace(/^[#]/g, "");

    const code = derive_channel_address_from_name(channel_name);
    CHANNEL_CODES[channel_name] = code;
    channels[code] = [];
    last_read_of_channel[code] = 0;

    const $channel =
        $('<div>').addClass('channel').attr("id", "channel_"+CHANNEL_CODES[channel_name]).text("#"+channel_name)
            .append($('<label>').addClass('new_msg_counter').text(""))
            .click(function () { change_channel(channel_name); });
    $('#channels').append($channel);
}

function derive_channel_address_from_name(channel_name) {
    return channel_name.trim().toUpperCase().replace(/[^a-zA-Z0-9]*/g, "").padEnd(81, "9").substr(0, 81);
}

function init() {

    $('#loading_page').removeClass("hidden");

    $.ajax({
        dataType: "json",
        url: REST_URL_INIT,
        method: 'POST',
        data: [{'name': 'history_size', 'value': settings['history_size']}, {'name': 'password', 'value': settings['password']}],
        success: function (initial_channels) {
            initial_channels.sort().forEach(function(channel) { add_channel_internally(channel); });
            if(!initialized) {
                change_channel(initial_channels.includes("speculation") ? "speculation" : initial_channels[0]);
                read_message();
            }
            initialized = true;
            update_online_users();
            submit_life_sign();
            $('#loading_page').addClass("hidden");
        },
        error: function (err) {
            $('#loading_page').addClass("hidden");
            const msg = "Could not connect to <code>" + REST_URL + "</code><br/><br/>"+JSON.stringify(err) + "</b><br/><br/>Maybe you got the password wrong? Let's try again.";
            swal("Failed to connecto to API", msg, "warning").then(ask_for_password_and_connect);
        }
    });
}

function read_message() {
    $.ajax({
        dataType: "json",
        method: 'POST',
        data: [{'name': 'password', 'value': settings['password']}],
        url: REST_URL_GET,
        success: function (txs) {
            console.log("received " + txs.length);
            txs.forEach(function (tx) {
                new_message(tx);
            });
            read_message();
            if(txs.length > 0)
                audio.play();
        },
        error: function (err) { console.log(err) }
    });
}


function ask_for_password_and_connect() {
    swal({
        title: 'Enter password for API',
        input: 'password',
    }).then(function (text) {
        settings['password'] = text.value;
        save_settings();
        apply_settings();
    })
}


function submit_life_sign() {

    if(REST_URL === undefined)
        return;

    const cookie_name = "last_life_sign";
    const c_last_life_sign = get_cookie(cookie_name);
    let lls = c_last_life_sign === undefined || c_last_life_sign === "" ? last_life_sign : c_last_life_sign;

    console.log(lls  + ", " + (new Date() - lls));
    if((new Date() - lls < 240000))
        return;

    $.ajax({
        url: REST_URL_SUBMIT+"LIFESIGN".padEnd(81, "9")+"/",
        method: 'POST',
        data: [{"name": "message", "value":  ""}, {'name': 'password', 'value': settings['password']}],
        success: function() { last_life_sign = (new Date()-1); set_cookie(cookie_name, last_life_sign + ""); },
        error: function (err) { console.log(err); },
    });
}

function submit() {
    const message = $('#message').val();
    if(message.length > 0)
        submit_message(current_channel, message);
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

function user_id_dialog(callback) {

    swal({
        title: 'Enter User ID',
        input: 'text'
    }).then(function (text) {

        let user_id = text.value;
        if(user_id.startsWith("@"))
            user_id = user_id.substr(1);
        if(!user_id || user_id.length !== 8)
            return swal("", "User ID must be 8 trytes long!", "error");
        callback(user_id)
    })
}

function add_contact(user_id) {

    $('#msgs').html("");


    $.ajax({
        url: REST_URL_ADD_CONTACT + user_id,
        method: 'POST',
        data: [{'name': 'password', 'value': settings['password']}],
        success: function(data) {
            for(var channel in channels) {
                channels[channel].forEach(function (msg) {
                    if(msg['user_id'] === user_id)
                        msg['is_trusted'] = true;
                })
            }
            if(online_users[user_id] !== undefined)
                online_users[user_id]['is_trusted'] = true;
            show_all_messages();
            show_online_users();
            swal("Contact added!", "All messages of user <b>@"+user_id+"</b> will now be marked with a white border.", "success");
        },
        error: function (err) { console.log(err); },
    });
}

function remove_contact(user_id) {

    $('#msgs').html("");

    $.ajax({
        url: REST_URL_REMOVE_CONTACT + user_id,
        method: 'POST',
        data: [{'name': 'password', 'value': settings['password']}],
        success: function(data) {
            for(var channel in channels) {
                channels[channel].forEach(function (msg) {
                    if(msg['user_id'] === user_id)
                        msg['is_trusted'] = false;
                })
            }
            if(online_users[user_id] !== undefined)
                online_users[user_id]['is_trusted'] = false;
            show_all_messages();
            show_online_users();
            swal("Contact removed!", "User <b>@"+user_id+"</b> was removed from your contacts.", "success");
        },
        error: function (err) { console.log(err); },
    });
}

function update_online_users() {
    if(REST_URL_GET_ONLINE_USERS === undefined)
        return;
    $.ajax({
        dataType: "json",
        method: 'POST',
        data: [{'name': 'password', 'value': settings['password']}],
        url: REST_URL_GET_ONLINE_USERS,
        success: function (data) {
            online_users = data;
            show_online_users();
        },
        error: function (err) { console.log(err); },
    });
}

function channel_dialog(callback) {
    swal({
        title: 'Enter Channel Name',
        input: 'text'
    }).then(function (text) {
        let channel_name = text.value;
        callback(channel_name);
    })
}

function copy(name, content) {
    $('#copy').val(content);
    document.getElementById('copy').select();
    document.execCommand("copy");
    swal("", "<b>"+name+"</b> copied to clipboard!", "success");
}