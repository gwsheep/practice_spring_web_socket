let client = null;
let roomSubscription = null;
let currentRoomId = null;

const STORAGE_KEY_CHAT_SESSION = "chat.session";
const CHAT_SESSION_TTL_MS = 30 * 60 * 1000;

function connect(afterConnectCallback) {
    if (client && client.active) {
        console.log("Already connected");

        if (client.connected && typeof afterConnectCallback === "function") {
            afterConnectCallback();
        }

        return;
    }

    client = new StompJs.Client({
        brokerURL: "ws://localhost:8080/ws",

        debug: function (str) {
            console.log(str);
        },

        onConnect: function () {
            document.getElementById("connectionStatus").textContent = "CONNECTED";
            console.log("WebSocket connected");

            if (typeof afterConnectCallback === "function") {
                afterConnectCallback();
            }
        },

        onDisconnect: function () {
            document.getElementById("connectionStatus").textContent = "DISCONNECTED";
            console.log("WebSocket disconnected");
        },

        onStompError: function (frame) {
            console.error("STOMP error", frame);
        }
    });

    client.activate();
}

async function joinRoom() {
    if (!client || !client.connected) {
        alert("먼저 Connect 버튼을 눌러주세요.");
        return;
    }

    const sender = document.getElementById("sender").value.trim();
    const roomId = document.getElementById("roomId").value.trim();

    if (!sender) {
        alert("sender를 입력해주세요.");
        return;
    }

    if (!roomId) {
        alert("roomId를 입력해주세요.");
        return;
    }

    if (roomSubscription) {
        roomSubscription.unsubscribe();
        roomSubscription = null;
    }

    currentRoomId = roomId;

    saveChatSession(sender, roomId);

    document.getElementById("currentRoom").textContent = roomId;
    document.getElementById("messages").innerHTML = "";

    // 이전 메시지 조회 기능 붙일 경우 여기에서 호출
    await loadHistory(roomId);

    roomSubscription = client.subscribe("/topic/rooms/" + roomId, function (message) {
        const body = JSON.parse(message.body);
        appendMessage(body);
    });

    client.publish({
        destination: "/app/socket/user/chat",
        body: JSON.stringify({
            roomId: currentRoomId,
            sender: sender,
            type: "ENTER",
            message: sender + "님이 입장했습니다."
        })
    });

    console.log("Joined room:", roomId);
}

function saveChatSession(sender, roomId) {
    const session = {
        sender: sender,
        roomId: roomId,
        expiresAt: Date.now() + CHAT_SESSION_TTL_MS
    }
    sessionStorage.setItem(STORAGE_KEY_CHAT_SESSION, JSON.stringify(session));
}

function getValidChatSession() {
    const raw = sessionStorage.getItem(STORAGE_KEY_CHAT_SESSION);

    if (!raw) {
        return null;
    }

    try {
        const session = JSON.parse(raw);

        if (!session.sender || !session.roomId || !session.expiresAt) {
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

function sendMessage() {
    if (!validateChatReady()) {
        return;
    }

    const sender = document.getElementById("sender").value.trim();
    const message = document.getElementById("message").value.trim();

    if (!message) {
        alert("메시지를 입력해주세요.");
        return;
    }

    refreshChatSessionExpiration();

    client.publish({
        destination: "/app/socket/user/chat",
        body: JSON.stringify({
            roomId: currentRoomId,
            sender: sender,
            type: "TEXT",
            message: message
        })
    });

    document.getElementById("message").value = "";
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
        const uploadResponse = await fetch("/socket/file/upload", {
            method: "POST",
            body: formData
        });

        if (!uploadResponse.ok) {
            alert("파일 업로드 실패");
            return;
        }

        const uploadedFile = await uploadResponse.json();
        const sender = document.getElementById("sender").value.trim();

        const type = uploadedFile.contentType && uploadedFile.contentType.startsWith("image/")
            ? "IMAGE"
            : "FILE";

        client.publish({
            destination: "/app/socket/user/chat",
            body: JSON.stringify({
                roomId: currentRoomId,
                sender: sender,
                type: type,
                message: type === "IMAGE" ? "이미지를 보냈습니다." : "파일을 보냈습니다.",
                fileId: uploadedFile.fileId
            })
        });

        fileInput.value = "";
    } catch (e) {
        console.error("파일 전송 중 오류:", e);
        alert("파일 전송 중 오류가 발생했습니다.");
    }
}

function appendMessage(body) {
    const li = document.createElement("li");
    li.className = "bubble";

    if (body.type === "ENTER") {
        li.className = "bubble system";

        const enter = document.createElement("div");
        enter.textContent = "[입장] " + body.message;

        li.appendChild(enter);
        document.getElementById("messages").appendChild(li);
        return;
    }

    if (body.type === "LEAVE") {
        li.className = "bubble system";

        const leave = document.createElement("div");
        leave.textContent = "[퇴장] " + body.message;

        li.appendChild(leave);
        document.getElementById("messages").appendChild(li);
        return;
    }

    if (body.type === "NOTICE") {
        li.className = "bubble notice";

        const notice = document.createElement("div");
        notice.textContent = "[공지] " + body.message;

        li.appendChild(notice);
        document.getElementById("messages").appendChild(li);
        return;
    }

    const sender = document.createElement("div");
    sender.className = "sender";
    sender.textContent = "[Room " + body.roomId + "] " + body.sender;

    li.appendChild(sender);

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
        fileBox.appendChild(img);
        fileBox.appendChild(download);

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
        fileBox.appendChild(download);

        li.appendChild(text);
        li.appendChild(fileBox);
    }

    else {
        const unknown = document.createElement("pre");
        unknown.textContent = JSON.stringify(body, null, 2);
        li.appendChild(unknown);
    }

    document.getElementById("messages").appendChild(li);
}

function validateChatReady() {
    if (!client || !client.connected) {
        alert("WebSocket이 연결되지 않았습니다.");
        return false;
    }

    if (!currentRoomId) {
        alert("먼저 방에 입장해주세요.");
        return false;
    }

    return true;
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

function disconnect() {
    const sender = document.getElementById("sender").value.trim();

    if (client && client.connected && currentRoomId) {
        client.publish({
            destination: "/app/socket/user/chat",
            body: JSON.stringify({
                roomId: currentRoomId,
                sender: sender,
                type: "LEAVE",
                message: sender + "님이 퇴장했습니다."
            })
        });
    }

    if (roomSubscription) {
        roomSubscription.unsubscribe();
        roomSubscription = null;
    }

    if (client) {
        client.deactivate();
        console.log("Disconnected manually");
    }

    clearChatSession();

    currentRoomId = null;
    document.getElementById("currentRoom").textContent = "NONE";
    document.getElementById("connectionStatus").textContent = "DISCONNECTED";
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

window.addEventListener("load", function () {

    const session = getValidChatSession();

    if(!session) {
        return;
    }

    document.getElementById("sender").value = session.sender;
    document.getElementById("roomId").value = session.roomId;

    connect(function () {
        joinRoom();
    });
});

async function loadHistory(roomId) {
    try {
        const response = await fetch("/api/rooms/" + roomId + "/messages");

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