const CHAT_ROOM_ID_KEY = "chat.roomId";

async function login() {
    const username = document.getElementById("userName").value.trim();

    if (!username) {
        alert("username을 입력해주세요.");
        return;
    }

    const response = await fetch("/auth/login", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            user: username
        })
    });

    if (!response.ok) {
        alert("로그인 실패");
        return;
    }

    const data = await response.json();

    sessionStorage.setItem("accessToken", data.accessToken);
    sessionStorage.setItem("userName", username);

    const userNameInput = document.getElementById("userName");
    userNameInput.value = username;
    userNameInput.readOnly = true;

    document.getElementById("loginStatus").textContent = "LOGIN";

    alert("로그인 성공");
}

function enterChatRoom() {
    const token = sessionStorage.getItem("accessToken");
    const userName = sessionStorage.getItem("userName");
    const roomId = document.getElementById("roomId").value.trim();

    if (!token || !userName) {
        alert("로그인 먼저 해주세요.");
        return;
    }

    if (!roomId) {
        alert("roomId를 입력해주세요.");
        return;
    }

    sessionStorage.setItem(CHAT_ROOM_ID_KEY, roomId);

    document.getElementById("selectedRoom").textContent = roomId;

    location.href = "/chat.html";
}

document.addEventListener("DOMContentLoaded", function () {
    const storedUserName = sessionStorage.getItem("userName");
    const storedRoomId = sessionStorage.getItem(CHAT_ROOM_ID_KEY);

    if (storedUserName) {
        const userNameInput = document.getElementById("userName");
        userNameInput.value = storedUserName;
        userNameInput.readOnly = true;

        document.getElementById("loginStatus").textContent = "LOGIN";
    }

    if (storedRoomId) {
        document.getElementById("roomId").value = storedRoomId;
        document.getElementById("selectedRoom").textContent = storedRoomId;
    }
});

async function loadActiveRooms() {
    const activeRooms = document.getElementById("activeRooms");

    if (!activeRooms) {
        return;
    }

    activeRooms.innerHTML = "";

    try {
        const response = await fetch("/socket/rooms/list", {
            headers: {
                Authorization: "Bearer " + sessionStorage.getItem("accessToken")
            }
        });

        if (!response.ok) {
            console.warn("방 목록 조회 실패:", response.status);
            activeRooms.innerHTML = "<li>방 목록을 불러오지 못했습니다.</li>";
            return;
        }

        const rooms = await response.json();

        if (!rooms || rooms.length === 0) {
            activeRooms.innerHTML = "<li>현재 활성화된 방이 없습니다.</li>";
            return;
        }

        rooms.forEach(function (roomId) {
            const li = document.createElement("li");

            const button = document.createElement("button");
            button.type = "button";
            button.className = "active-room-button";
            button.textContent = "Room " + roomId;
            button.onclick = function () {
                selectRoom(roomId);
            };

            li.appendChild(button);
            activeRooms.appendChild(li);
        });

    } catch (e) {
        console.error("방 목록 조회 중 오류:", e);
        activeRooms.innerHTML = "<li>방 목록 조회 중 오류가 발생했습니다.</li>";
    }
}

function selectRoom(roomId) {
    const roomIdInput = document.getElementById("roomId");
    const selectedRoom = document.getElementById("selectedRoom");

    if (roomIdInput) {
        roomIdInput.value = roomId;
    }

    if (selectedRoom) {
        selectedRoom.textContent = roomId;
    }
}