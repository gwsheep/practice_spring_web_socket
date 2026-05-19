let stompClient = null;
let roomSubscription = null;
let userListSubscription = null;
let currentRoomId = null;

const STORAGE_KEY_CHAT_SESSION = "chat.session";
const CHAT_SESSION_TTL_MS = 30 * 60 * 1000;

const CHAT_ROOM_ID_KEY = "chat.roomId";

const WS_BROKER_URL = "ws://localhost:8080/ws";
const CHAT_PUBLISH_DESTINATION = "/app/socket/user/chat";
const ROOM_TOPIC_PREFIX = "/topic/rooms/";
const FILE_UPLOAD_URL = "/socket/file/upload";
const HISTORY_API_PREFIX = "/api/rooms/";
const HISTORY_API_SUFFIX = "/messages";

function getAccessToken() {
    return sessionStorage.getItem("accessToken");
}

function getCurrentUserName() {
    const storedUserName = sessionStorage.getItem("userName");

    if (storedUserName) {
        return storedUserName;
    }

    return "";
}

function connect(afterConnectCallback) {
    const token = getAccessToken();

    if (!token) {
        alert("로그인 먼저 해주세요.");
        location.href = "/lobby.html";
        return;
    }

    if (stompClient && stompClient.active) {
        console.log("Already connected");

        if (stompClient.connected && typeof afterConnectCallback === "function") {
            afterConnectCallback();
        }

        return;
    }

    stompClient = new StompJs.Client({
        brokerURL: WS_BROKER_URL,

        connectHeaders: {
            Authorization: "Bearer " + token
        },

        debug: function (str) {
            console.log(str);
        },

        onConnect: function () {
            const connectionStatus = document.getElementById("connectionStatus");

            if (connectionStatus) {
                connectionStatus.textContent = "CONNECTED";
            }

            console.log("STOMP connected");

            if (typeof afterConnectCallback === "function") {
                afterConnectCallback();
            }
        },

        onWebSocketClose: function () {
            const connectionStatus = document.getElementById("connectionStatus");

            if (connectionStatus) {
                connectionStatus.textContent = "DISCONNECTED";
            }

            console.log("WebSocket closed");
        },

        onDisconnect: function () {
            const connectionStatus = document.getElementById("connectionStatus");

            if (connectionStatus) {
                connectionStatus.textContent = "DISCONNECTED";
            }

            console.log("STOMP disconnected");
        },

        onStompError: function (frame) {
            console.error("STOMP error:", frame.headers["message"]);
            console.error("STOMP error detail:", frame.body);
            alert("WebSocket 인증 또는 연결 오류가 발생했습니다.");
            location.href = "/lobby.html";
        }
    });

    stompClient.activate();
}

function subscribeRoom(roomId) {
    if (!stompClient || !stompClient.connected) {
        alert("WebSocket이 연결되지 않았습니다.");
        return;
    }

    if (roomSubscription) {
        roomSubscription.unsubscribe();
        roomSubscription = null;
    }

    if (userListSubscription) {
        userListSubscription.unsubscribe();
        userListSubscription = null;
    }

    roomSubscription = stompClient.subscribe(ROOM_TOPIC_PREFIX + roomId, function (message) {
        const body = JSON.parse(message.body);
        appendMessage(body);
    });

    userListSubscription = stompClient.subscribe(ROOM_TOPIC_PREFIX + roomId + "/users", function (message) {
        const users = JSON.parse(message.body);
        renderOnlineUsers(users);
    });

    currentRoomId = roomId;

    const currentRoom = document.getElementById("currentRoom");

    if (currentRoom) {
        currentRoom.textContent = roomId;
    }

    console.log("Subscribed room:", roomId);
}

async function enterRoom(roomId) {
    if (!roomId) {
        alert("roomId가 없습니다.");
        location.href = "/lobby.html";
        return;
    }

    if (!stompClient || !stompClient.connected) {
        alert("WebSocket이 연결되지 않았습니다.");
        return;
    }

    saveChatSession(roomId);

    const messages = document.getElementById("messages");

    if (messages) {
        messages.innerHTML = "";
    }

    subscribeRoom(roomId);

    await loadHistory(roomId);

    publishChatMessage({
        roomId: roomId,
        type: "ENTER",
        message: "입장했습니다."
    });

    console.log("Entered room:", roomId);
}

function publishChatMessage(payload) {
    if (!stompClient || !stompClient.connected) {
        alert("WebSocket이 연결되지 않았습니다.");
        return;
    }

    stompClient.publish({
        destination: CHAT_PUBLISH_DESTINATION,
        body: JSON.stringify(payload)
    });
}

function sendMessage() {
    if (!validateChatReady()) {
        return;
    }

    const messageInput = document.getElementById("message");
    const message = messageInput.value.trim();

    if (!message) {
        alert("메시지를 입력해주세요.");
        return;
    }

    refreshChatSessionExpiration();

    publishChatMessage({
        roomId: currentRoomId,
        type: "TEXT",
        message: message
    });

    messageInput.value = "";
}

async function sendFile() {
    if (!validateChatReady()) {
        return;
    }

    const fileInput = document.getElementById("fileInput");
    const file = fileInput.files[0];

    if (!file) {
        alert("파일을 선택해주세요.");
        return;
    }

    refreshChatSessionExpiration();

    const formData = new FormData();
    formData.append("file", file);

    try {
        const uploadResponse = await fetch(FILE_UPLOAD_URL, {
            method: "POST",
            headers: {
                Authorization: "Bearer " + getAccessToken()
            },
            body: formData
        });

        if (!uploadResponse.ok) {
            alert("파일 업로드 실패");
            return;
        }

        const uploadedFile = await uploadResponse.json();

        const type = uploadedFile.contentType && uploadedFile.contentType.startsWith("image/")
            ? "IMAGE"
            : "FILE";

        publishChatMessage({
            roomId: currentRoomId,
            type: type,
            message: type === "IMAGE" ? "이미지를 보냈습니다." : "파일을 보냈습니다.",
            fileId: uploadedFile.fileId,
            fileName: uploadedFile.fileName,
            fileSize: uploadedFile.fileSize,
            downloadUrl: uploadedFile.downloadUrl
        });

        clearSelectedFile();

    } catch (e) {
        console.error("파일 전송 중 오류:", e);
        alert("파일 전송 중 오류가 발생했습니다.");
    }
}

function appendMessage(body) {
    const messages = document.getElementById("messages");

    if (!messages) {
        return;
    }

    const li = document.createElement("li");
    li.className = "bubble";

    if (body.type === "ENTER") {
        li.className = "bubble system enter";

        const enter = document.createElement("div");
        enter.textContent = "[입장] " + body.message;

        li.appendChild(enter);
        messages.appendChild(li);
        scrollToBottom();
        return;
    }

    if (body.type === "LEAVE") {
        const currentUserName = getCurrentUserName();

        if (body.userName === currentUserName) {
            return;
        }

        li.className = "bubble system leave";

        const leave = document.createElement("div");
        leave.textContent = "[퇴장] " + body.message;

        li.appendChild(leave);
        messages.appendChild(li);
        scrollToBottom();
        return;
    }

    if (body.type === "NOTICE") {
        li.className = "bubble notice";

        const notice = document.createElement("div");
        notice.textContent = "[공지] " + body.message;

        li.appendChild(notice);
        messages.appendChild(li);
        scrollToBottom();
        return;
    }

    if (body.type === "CLOSED") {
        li.className = "bubble system closed";

        const closed = document.createElement("div");
        closed.textContent = body.message;

        li.appendChild(closed);
        messages.appendChild(li);
        scrollToBottom();
        return;
    }

    const currentUserName = getCurrentUserName();

    if (body.userName === currentUserName) {
        li.className = "bubble my-message";
    } else {
        li.className = "bubble other-message";
    }

    if (!body.type || body.type === "TEXT") {
        const text = document.createElement("div");
        text.textContent = body.message;
        li.appendChild(text);
    }

    else if (body.type === "IMAGE") {
        const text = document.createElement("div");
        text.textContent = body.message || "이미지를 보냈습니다.";

        const fileBox = document.createElement("div");
        fileBox.className = "file-box";

        const fileName = document.createElement("div");
        fileName.textContent = "이미지: " + (body.fileName || "unknown")
            + " (" + formatFileSize(body.fileSize) + ")";

        const img = document.createElement("img");
        img.src = body.downloadUrl;
        img.className = "image-preview";

        const download = document.createElement("a");
        download.href = body.downloadUrl;
        download.textContent = "다운로드";
        download.target = "_blank";

        fileBox.appendChild(fileName);

        if (body.downloadUrl) {
            fileBox.appendChild(img);
            fileBox.appendChild(download);
        }

        li.appendChild(text);
        li.appendChild(fileBox);
    }

    else if (body.type === "FILE") {
        const text = document.createElement("div");
        text.textContent = body.message || "파일을 보냈습니다.";

        const fileBox = document.createElement("div");
        fileBox.className = "file-box";

        const fileInfo = document.createElement("div");
        fileInfo.textContent = "파일: " + (body.fileName || "unknown")
            + " (" + formatFileSize(body.fileSize) + ")";

        const download = document.createElement("a");
        download.href = body.downloadUrl;
        download.textContent = "다운로드";
        download.target = "_blank";

        fileBox.appendChild(fileInfo);

        if (body.downloadUrl) {
            fileBox.appendChild(download);
        }

        li.appendChild(text);
        li.appendChild(fileBox);
    }

    else {
        const unknown = document.createElement("pre");
        unknown.textContent = JSON.stringify(body, null, 2);
        li.appendChild(unknown);
    }

    messages.appendChild(li);
    scrollToBottom();
}

function renderOnlineUsers(users) {
    const onlineUsers = document.getElementById("onlineUsers");

    if (!onlineUsers) {
        return;
    }

    onlineUsers.innerHTML = "";

    if (!users || users.length === 0) {
        const li = document.createElement("li");
        li.textContent = "접속자가 없습니다.";
        onlineUsers.appendChild(li);
        return;
    }

    const currentUserName = getCurrentUserName();

    users.forEach(function (user) {
        const userName = typeof user === "string" ? user : user.userName;

        const li = document.createElement("li");
        li.textContent = userName || "unknown";

        if (userName === currentUserName) {
            li.classList.add("me");
        }

        onlineUsers.appendChild(li);
    });
}

function leaveRoom() {
    if (stompClient && stompClient.connected && currentRoomId) {
        publishChatMessage({
            roomId: currentRoomId,
            type: "LEAVE",
            message: "퇴장했습니다."
        });
    }

    if (roomSubscription) {
        roomSubscription.unsubscribe();
        roomSubscription = null;
    }

    if (userListSubscription) {
        userListSubscription.unsubscribe();
        userListSubscription = null;
    }

    clearChatSession();
    sessionStorage.removeItem(CHAT_ROOM_ID_KEY);

    currentRoomId = null;

    const currentRoom = document.getElementById("currentRoom");
    const connectionStatus = document.getElementById("connectionStatus");

    if (currentRoom) {
        currentRoom.textContent = "NONE";
    }

    if (connectionStatus) {
        connectionStatus.textContent = "DISCONNECTED";
    }

    renderOnlineUsers([]);

    if (stompClient) {
        stompClient.deactivate();
        stompClient = null;
        console.log("Disconnected manually");
    }

    location.href = "/lobby.html";
}

async function loadHistory(roomId) {
    try {
        const response = await fetch(
            HISTORY_API_PREFIX + encodeURIComponent(roomId) + HISTORY_API_SUFFIX,
            {
                headers: {
                    Authorization: "Bearer " + getAccessToken()
                }
            }
        );

        if (!response.ok) {
            console.warn("이전 메시지 조회 실패:", response.status);
            return;
        }

        const historyMessages = await response.json();

        historyMessages.forEach(function (message) {
            appendMessage(message);
        });

    } catch (e) {
        console.error("이전 메시지 조회 중 오류:", e);
    }
}

function validateChatReady() {
    if (!stompClient || !stompClient.connected) {
        alert("WebSocket이 연결되지 않았습니다.");
        return false;
    }

    if (!currentRoomId) {
        alert("먼저 방에 입장해주세요.");
        return false;
    }

    return true;
}

function saveChatSession(roomId) {
    const session = {
        roomId: roomId,
        expiresAt: Date.now() + CHAT_SESSION_TTL_MS
    };

    sessionStorage.setItem(STORAGE_KEY_CHAT_SESSION, JSON.stringify(session));
}

function getValidChatSession() {
    const raw = sessionStorage.getItem(STORAGE_KEY_CHAT_SESSION);
    const token = getAccessToken();

    if (!raw || !token) {
        return null;
    }

    try {
        const session = JSON.parse(raw);

        if (!session.roomId || !session.expiresAt) {
            clearChatSession();
            return null;
        }

        if (Date.now() > session.expiresAt) {
            clearChatSession();
            return null;
        }

        return session;

    } catch (e) {
        clearChatSession();
        return null;
    }
}

function clearChatSession() {
    sessionStorage.removeItem(STORAGE_KEY_CHAT_SESSION);
}

function refreshChatSessionExpiration() {
    const session = getValidChatSession();

    if (!session) {
        return;
    }

    session.expiresAt = Date.now() + CHAT_SESSION_TTL_MS;
    sessionStorage.setItem(STORAGE_KEY_CHAT_SESSION, JSON.stringify(session));
}

function clearSelectedFile() {
    const fileInput = document.getElementById("fileInput");
    const selectedFileName = document.getElementById("selectedFileName");

    if (fileInput) {
        fileInput.value = "";
    }

    if (selectedFileName) {
        selectedFileName.textContent = "";
        selectedFileName.style.display = "none";
    }
}

function formatFileSize(size) {
    if (!size) {
        return "0 B";
    }

    if (size < 1024) {
        return size + " B";
    }

    if (size < 1024 * 1024) {
        return Math.round(size / 1024) + " KB";
    }

    return (size / 1024 / 1024).toFixed(1) + " MB";
}

function initFileInput() {
    const fileInput = document.getElementById("fileInput");

    if (!fileInput) {
        return;
    }

    fileInput.addEventListener("change", function () {
        const selectedFileName = document.getElementById("selectedFileName");

        if (!selectedFileName) {
            return;
        }

        if (this.files && this.files.length > 0) {
            selectedFileName.textContent = this.files[0].name;
            selectedFileName.style.display = "inline-block";
        } else {
            selectedFileName.textContent = "";
            selectedFileName.style.display = "none";
        }
    });
}

function scrollToBottom() {
    const messages = document.getElementById("messages");

    if (!messages) {
        return;
    }

    messages.scrollTop = messages.scrollHeight;
}

function initChatPage() {
    const token = getAccessToken();
    const userName = getCurrentUserName();
    const roomId = sessionStorage.getItem(CHAT_ROOM_ID_KEY);

    if (!token || !userName) {
        alert("로그인이 필요합니다.");
        location.href = "/lobby.html";
        return;
    }

    if (!roomId) {
        alert("입장할 방 정보가 없습니다.");
        location.href = "/lobby.html";
        return;
    }

    const currentUser = document.getElementById("currentUser");
    const currentRoom = document.getElementById("currentRoom");

    if (currentUser) {
        currentUser.textContent = userName;
    }

    if (currentRoom) {
        currentRoom.textContent = roomId;
    }

    connect(function () {
        enterRoom(roomId);
    });
}

window.addEventListener("beforeunload", function () {
    if (stompClient && stompClient.connected && currentRoomId) {
        publishChatMessage({
            roomId: currentRoomId,
            type: "LEAVE",
            message: "퇴장했습니다."
        });
    }
});

document.addEventListener("DOMContentLoaded", function () {
    initFileInput();
    initChatPage();
});