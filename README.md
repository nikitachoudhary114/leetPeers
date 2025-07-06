# LeetPeers 👥🔥

A full-stack social productivity platform where users can form coding groups, track LeetCode streaks, engage in real-time chats, compete with others, and collaborate with tools like video calls and whiteboards — all enhanced with AI.

---

## 🚀 Features

### 👨‍💻 Core Functionality
- ✅ **Group Creation & Join** – Users can create or join groups using unique invite codes.
- 📈 **LeetCode Integration** – Daily problem tracking per user via LeetCode API.
- 🔥 **Streak System** – 2+ problems/day required to maintain streaks. Group-based and individual.
- 🏅 **Badges** – 50-day and 100-day streak milestones with shareable badges.

### 💬 Communication
- 💬 **Real-Time Chat** – Built with **Socket.io** for instant group messaging.
- 🎥 **Video & Audio Calls** – WebRTC/Agora for real-time communication.
- ✏️ **Collaborative Whiteboard** – Excalidraw for brainstorming and teaching.

### 🤖 AI Assistant
- 💡 GPT-3 or Codex-based assistant for:
  - Debugging
  - Suggesting better solutions
  - Explaining code
  - Giving daily problem recommendations

---

## 🛠️ Tech Stack

| Layer       | Tech                     |
|-------------|--------------------------|
| Frontend    | Angular.js, Tailwind CSS |
| Backend     | Spring Boot (Java)       |
| Auth        | Google OAuth + JWT       |
| DB          | MySQL                    |
| Real-Time   | Socket.io, WebRTC        |
| AI API      | OpenAI (GPT-3/Codex)     |
| Deployment  | Render / Vercel / Railway|
| Caching     | Redis                    |
| Containers  | Docker, Docker Compose   |


---



## ⚙️ Setup Instructions

### 🔧 Frontend
```bash
cd frontend
npm install
ng serve

```

### 🔧 Backend

```bash
cd backend
# Setup .env or application.properties with DB & OAuth secrets
./mvnw spring-boot:run
