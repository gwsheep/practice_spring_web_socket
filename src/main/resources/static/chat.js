let client = null;
let roomSubscription = null;
let currentRoomId = null;

function connect() {
    if (client && client.active) {
        alert("Already connected");
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

    const roomId = document.getElementById("roomId").value;

    if (!roomId) {
        alert("roomId를 입력해주세요.");
        return;
    }

    if (roomSubscription) {
        roomSubscription.unsubscribe();
        roomSubscription = null;
    }

    currentRoomId = roomId;

    document.getElementById("currentRoom").textContent = roomId;
    document.getElementById("messages").innerHTML = "";

    // 이전 메시지 불러오기
    // await loadHistory(roomId);

    roomSubscription = client.subscribe("/topic/rooms/" + roomId, function (message) {
        const body = JSON.parse(message.body);
        appendMessage(body);
    });

    console.log("Joined room:", roomId);
}

// async function loadHistory(roomId) {
//     try {
//         const response = await fetch("/api/rooms/" + roomId + "/messages");
//
//         if (!response.ok) {
//             console.warn("이전 메시지 조회 실패:", response.status);
//             return;
//         }
//
//         const historyMessages = await response.json();
//
//         historyMessages.forEach(function (message) {
//             appendMessage(message);
//         });
//     } catch (e) {
//         console.error("이전 메시지 조회 중 오류:", e);
//     }
// }

function sendMessage() {
    if (!validateChatReady()) {
        return;
    }

    const sender = document.getElementById("sender").value;
    const message = document.getElementById("message").value;

    if (!message) {
        alert("메시지를 입력해주세요.");
        return;
    }

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
        const sender = document.getElementById("sender").value;

        const type = uploadedFile.contentType && uploadedFile.contentType.startsWith("image/")
            ? "IMAGE"
            : "FILE";

        client.publish({
            destination: "/app/socket/user/chat",
            body: JSON.stringify({
                roomId: currentRoomId,
                sender: sender,
                type: type,
                message: "파일을 보냈습니다.",
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

    else if (body.type === "NOTICE") {
        const notice = document.createElement("div");
        notice.textContent = "[공지] " + body.message;
        li.appendChild(notice);
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

function disconnect() {
    if (roomSubscription) {
        roomSubscription.unsubscribe();
        roomSubscription = null;
    }

    if (client) {
        client.deactivate();
        console.log("Disconnected manually");
    }

    currentRoomId = null;
    document.getElementById("currentRoom").textContent = "NONE";
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