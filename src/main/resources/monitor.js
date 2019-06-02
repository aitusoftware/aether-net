/*
 * Copyright 2019 Aitu Software Limited.
 *
 * https://aitusoftware.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
var aetherSocket;

function startMonitor() {
    if (window.WebSocket) {
        aetherSocket = new WebSocket("ws://localhost:8080/aether");
        aetherSocket.onmessage = function(event) {
            renderStreamData(JSON.parse(event.data));
        }
        aetherSocket.onopen = function(event) {
        }
        aetherSocket.onclose = function(event) {
        }
    } else {
        alert("WebSockets not supported.");
    }

    window.setInterval(pollData, 100);
}

function systemStatRow(name, value) {
    return '<div class="row"><div class="col-md-6 system-stat">' + name +
        '</div><div class="col-md-6 system-stat">' + value + '</div></div>';
}

function pubStatRow(name, value, extClass, bottomRow) {
    return '<div class="row pub-data ' + (bottomRow ? 'bottom-bar' : '') +
        '"><div class="col-md-4"></div><div class="col-md-4 stat-label">' + name +
        '</div><div class="col-md-4 pub-stat ' + extClass + '">' + value + '</div></div>';
}

function subStatRow(name, value, extClass, bottomRow) {
    return '<div class="row sub-data ' + (bottomRow ? 'bottom-bar' : '') +
        '"><div class="col-md-4"></div><div class="col-md-4 stat-label">' + name +
        '</div><div class="col-md-4 sub-stat ' + extClass + '">' + value + '</div></div>';
}

function renderStreamData(allData) {
    var html = '';
    var systemCounters = allData['systemCounters'];
    for (var label in systemCounters) {
        html += '<div class="channel row bottom-bar top-bar"><div class="stream col-md-12">' + label + '</div></div>';
        var systemCounterSet = systemCounters[label];
        html += systemStatRow('Bytes sent', systemCounterSet.bytesSent);
        html += systemStatRow('Bytes received', systemCounterSet.bytesReceived);
        html += systemStatRow('NAKs sent', systemCounterSet.naksSent);
        html += systemStatRow('NAKs received', systemCounterSet.naksReceived);
        html += systemStatRow('Errors', systemCounterSet.errors);
        html += systemStatRow('Client timeouts', systemCounterSet.clientTimeouts);
    }
    var streamData = allData['streams'];
    for (var channel in streamData) {
        for (var streamId in streamData[channel]) {
            var isIpcChannel = channel.indexOf('aeron:ipc') >= 0;
            html += '<div class="channel row bottom-bar top-bar"><div class="stream col-md-12">' + channel + ' / ' + streamId + '</div></div>';
            var publisherSet = streamData[channel][streamId];
            for (var i = 0; i < publisherSet.length; i++) {
                var publisher = publisherSet[i];
                var bpeCls = publisher.backPressureEvents == 0 ? '' : 'pub-data-highlight';
                var backlogCls = publisher.sendBacklog == 0 ? '' : 'pub-data-highlight';
                html += pubStatRow('Context', publisher.label, '', false);
                html += pubStatRow('Session', publisher.sessionId, '', false);
                html += pubStatRow('Publisher Position', publisher.publisherPosition, '', false);
                html += pubStatRow('Publisher Limit', publisher.publisherLimit, '', false);
                if (!isIpcChannel) {
                    html += pubStatRow('Sender Position', publisher.senderPosition, '', false);
                    html += pubStatRow('Sender Limit', publisher.senderLimit, '', false);
                    html += pubStatRow('Queued', publisher.sendBacklog, backlogCls, false);
                    html += pubStatRow('Remaining Buffer', publisher.remainingBuffer, '', false);
                }
                for (var rate in publisher.publishRates) {
                    html += pubStatRow('Rate ' + rate, publisher.publishRates[rate], '', false);
                }
                html += pubStatRow('Back Pressure', publisher.backPressureEvents, bpeCls, true);
                var subscriberSet = publisher.subscribers;

                for (var j = 0; j < subscriberSet.length; j++) {
                    var subscriber = subscriberSet[j];
                    html += '<div class="row"><div class="col-md-12"></div></div>';
                    html += subStatRow('Context', subscriber.label, '', false);
                    if (!isIpcChannel) {
                        html += subStatRow('Receiver Position', subscriber.receiverPosition, '', false);
                        html += subStatRow('Receiver HWM', subscriber.receiverHighWaterMark, '', true);
                    }
                    for (var reg in subscriber.subscriberPositions) {
                        var available = 0;
                        if (isIpcChannel) {
                            available = Math.max(0, publisher.publisherPosition - subscriber.subscriberPositions[reg]);
                        } else {
                            available = Math.max(0, subscriber.receiverPosition - subscriber.subscriberPositions[reg]);
                        }

                        var cls = available > 0 ? 'sub-data-highlight' : '';
                        html += subStatRow('Subscriber Position', subscriber.subscriberPositions[reg], '', false);
                        html += subStatRow('Bytes Available', available, cls, true);
                    }
                }

            }
        }
    }

    document.getElementById('stream-data').innerHTML = html;
}

function pollData() {
    if (aetherSocket.readyState == WebSocket.OPEN) {
        aetherSocket.send('');
    } else {
        document.getElementById('stream-data').innerHTML = '<h1>Socket closed</h1>';
    }
}
