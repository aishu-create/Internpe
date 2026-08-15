const icons = ["🪨", "📄", "✂️", "❓", "✅"];

// UI Elements
const p1ChoiceEl = document.getElementById('p1-choice');
const p2ChoiceEl = document.getElementById('p2-choice');
const p1Result = document.getElementById('p1-result');
const p2Result = document.getElementById('p2-result');
const music = document.getElementById('bg-music');

// Game State
let mode = 'pvc'; 
let p1Selected = -1;
let p2Selected = -1;
let score1 = 0;
let score2 = 0;
let isMusicPlaying = false;

let p1Name = "Player 1";
let p2Name = "Computer";

// --- Custom Names Logic ---
function updateNames() {
    p1Name = document.getElementById('p1-name-input').value || "Player 1";
    
    if (mode === 'pvp') {
        p2Name = document.getElementById('p2-name-input').value || "Player 2";
        document.getElementById('p2-header').innerText = `👤 ${p2Name}`;
    } else {
        p2Name = "Computer";
        document.getElementById('p2-header').innerText = `🤖 Computer`;
    }

    document.getElementById('p1-header').innerText = `👤 ${p1Name}`;
    document.getElementById('p1-score-name').innerText = p1Name;
    document.getElementById('p2-name-score').innerText = p2Name;
}

// --- Feature Toggles ---
function toggleMusic() {
    if (isMusicPlaying) {
        music.pause();
        document.getElementById('music-btn').innerText = "🔇 Music Off";
        isMusicPlaying = false;
    } else {
        music.play().then(() => {
            document.getElementById('music-btn').innerText = "🔊 Music On";
            isMusicPlaying = true;
        }).catch(error => {
            console.log("Music play failed:", error);
            alert("Ensure you are connected to the internet to load the music track!");
        });
    }
}

function toggleMode() {
    if (mode === 'pvc') {
        mode = 'pvp';
        document.getElementById('mode-btn').innerText = "🎮 Mode: Player vs Player";
        document.getElementById('p2-controls').style.display = "flex";
        document.getElementById('p2-name-input').style.display = "inline-block";
    } else {
        mode = 'pvc';
        document.getElementById('mode-btn').innerText = "🎮 Mode: vs Computer";
        document.getElementById('p2-controls').style.display = "none";
        document.getElementById('p2-name-input').style.display = "none";
    }
    updateNames();
    resetGame();
}

// --- Gameplay Logic ---
function playP1(choice) {
    p1Selected = choice;
    p1ChoiceEl.innerText = icons[choice];
    p1Result.innerText = "";
    p2Result.innerText = "";
    
    if (mode === 'pvc') {
        p2ChoiceEl.innerText = "⏳";
        fetchResult(p1Selected, -1);
    } else {
        checkPvPReady();
    }
}

function playP2(choice) {
    if (mode !== 'pvp') return;
    p2Selected = choice;
    p2ChoiceEl.innerText = "✅"; 
    checkPvPReady();
}

function checkPvPReady() {
    if (p1Selected !== -1 && p2Selected !== -1) {
        fetchResult(p1Selected, p2Selected);
    }
}

// --- Server Communication & Results ---
async function fetchResult(p1, p2) {
    try {
        const response = await fetch(`/api/play?p1=${p1}&p2=${p2}`);
        const data = await response.json();

        // Reveal choices
        p2ChoiceEl.innerText = icons[data.p2Choice];

        let resultMessage = "";

        // Process Winner
        if (data.winner === 1) {
            resultMessage = `${p1Name.toUpperCase()} WINS! 🏆`;
            score1++;
            triggerCelebration();
            speakVoice(`${p1Name} wins`);
        } else if (data.winner === 2) {
            resultMessage = `${p2Name.toUpperCase()} WINS! 🏆`;
            score2++;
            if(mode === 'pvp') triggerCelebration(); 
            speakVoice(`${p2Name} wins`);
        } else {
            resultMessage = "TIE! 🤝";
            speakVoice("It's a tie");
        }

        // Update UI Text & Score
        p1Result.innerText = resultMessage;
        p2Result.innerText = resultMessage;
        document.getElementById('s1').innerText = score1;
        document.getElementById('s2').innerText = score2;

        p1Selected = -1;
        p2Selected = -1;

    } catch (error) {
        console.error("Error:", error);
        alert("Cannot connect to Java Backend. Make sure GameServer.java is running!");
    }
}

// --- Fun Effects ---
function triggerCelebration() {
    confetti({
        particleCount: 150,
        spread: 80,
        origin: { y: 0.6 }
    });
}

function speakVoice(text) {
    window.speechSynthesis.cancel();
    const speech = new SpeechSynthesisUtterance(text);
    speech.volume = 1;
    speech.rate = 1.0; 
    speech.pitch = 1.2; 
    window.speechSynthesis.speak(speech);
}

function resetGame() {
    p1ChoiceEl.innerText = "❓";
    p2ChoiceEl.innerText = "❓";
    p1Result.innerText = "";
    p2Result.innerText = "";
    p1Selected = -1;
    p2Selected = -1;
    score1 = 0;
    score2 = 0;
    document.getElementById('s1').innerText = score1;
    document.getElementById('s2').innerText = score2;
}