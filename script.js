document.addEventListener("DOMContentLoaded", () => {
    // --- UI Elements ---
    const ui = {
        setup: document.getElementById('setupModal'),
        game: document.getElementById('gameUI'),
        board: document.getElementById('board'),
        status: document.getElementById('statusText'),
        bgMusic: document.getElementById('bgMusic')
    };

    const inputs = {
        mode: document.getElementById('modeSelect'),
        p1Name: document.getElementById('p1Name'),
        p2Name: document.getElementById('p2Name')
    };

    // --- State ---
    let gameMode = 'pvsystem';
    let boardData = ["-", "-", "-", "-", "-", "-", "-", "-", "-"];
    let isGameActive = true;
    let currentPlayer = 'X';
    let scores = { X: 0, O: 0 };
    let names = { X: "", O: "" };
    let musicPlaying = false;

    // --- Setup Listeners ---
    inputs.mode.addEventListener('change', (e) => {
        inputs.p2Name.disabled = (e.target.value === 'pvsystem');
        inputs.p2Name.value = (e.target.value === 'pvp') ? "Player 2" : "System";
        document.getElementById('avatarP2').innerText = (e.target.value === 'pvp') ? "🥷🏼" : "🤖";
    });

    document.getElementById('startGameBtn').addEventListener('click', () => {
        gameMode = inputs.mode.value;
        names.X = inputs.p1Name.value || "Player 1";
        names.O = inputs.p2Name.value || "System";

        document.getElementById('displayP1').innerText = names.X;
        document.getElementById('displayP2').innerText = names.O;

        ui.setup.style.display = 'none';
        ui.game.style.display = 'block';

        // Start Music
        ui.bgMusic.volume = 0.3;
        ui.bgMusic.play().then(() => musicPlaying = true).catch(() => console.log("Music blocked by browser autoplay rules."));

        speak(`Match initialized. ${names.X} versus ${names.O}. Begin.`);
        buildBoard();
        updateTurnUI();
    });

    document.getElementById('musicToggle').addEventListener('click', (e) => {
        if (musicPlaying) { ui.bgMusic.pause(); e.target.innerText = "🔇 Music Off"; }
        else { ui.bgMusic.play(); e.target.innerText = "🔊 Music On"; }
        musicPlaying = !musicPlaying;
    });

    document.getElementById('resetRoundBtn').addEventListener('click', resetRound);

    // --- Voice Assistant ---
    function speak(text) {
        if ('speechSynthesis' in window) {
            window.speechSynthesis.cancel();
            const utterance = new SpeechSynthesisUtterance(text);
            utterance.rate = 1.0;
            window.speechSynthesis.speak(utterance);
        }
    }

    // --- Board Logic ---
    function buildBoard() {
        ui.board.innerHTML = '';
        boardData.forEach((_, i) => {
            const cube = document.createElement('div');
            cube.classList.add('cube');
            cube.dataset.index = i;
            cube.innerHTML = `<div class="cube-face cube-face-front"></div><div class="cube-face cube-face-back"></div>`;
            cube.addEventListener('click', () => handleMove(i, cube));
            ui.board.appendChild(cube);
        });
    }

    function handleMove(index, cube) {
        if (!isGameActive || boardData[index] !== "-") return;
        if (gameMode === 'pvsystem' && currentPlayer === 'O') return; // Prevent clicking during AI turn

        applyMove(index, cube, currentPlayer);

        if (!checkWin()) {
            currentPlayer = (currentPlayer === 'X') ? 'O' : 'X';
            updateTurnUI();

            if (gameMode === 'pvsystem' && currentPlayer === 'O') {
                ui.status.innerText = "System Computing...";
                fetchAIMoveFromJava();
            }
        }
    }

    function applyMove(index, cube, player) {
        boardData[index] = player;
        cube.classList.add('is-flipped', player === 'X' ? 'x-mark' : 'o-mark');
        cube.querySelector('.cube-face-back').innerText = player;
    }

    // --- Java Backend Integration ---
    async function fetchAIMoveFromJava() {
        try {
            const boardStr = boardData.join('');
            const response = await fetch(`http://localhost:8080/api/aimove?board=${boardStr}`);
            const aiIndex = await response.text();

            if (aiIndex !== "-1") {
                const aiCube = document.querySelector(`.cube[data-index='${aiIndex}']`);
                setTimeout(() => {
                    applyMove(parseInt(aiIndex), aiCube, 'O');
                    if (!checkWin()) {
                        currentPlayer = 'X';
                        updateTurnUI();
                    }
                }, 700);
            }
        } catch (error) {
            ui.status.innerText = "Error: Start Java Server!";
            speak("Error. Java Server is offline.");
        }
    }

    // --- Game Rules & Visuals ---
    function checkWin() {
        const combos = [[0,1,2],[3,4,5],[6,7,8],[0,3,6],[1,4,7],[2,5,8],[0,4,8],[2,4,6]];
        let winner = null;

        for (let c of combos) {
            if (boardData[c[0]] !== "-" && boardData[c[0]] === boardData[c[1]] && boardData[c[0]] === boardData[c[2]]) {
                winner = boardData[c[0]]; break;
            }
        }

        if (winner) {
            isGameActive = false;
            scores[winner]++;
            document.getElementById(`scoreP${winner === 'X' ? 1 : 2}`).innerText = scores[winner];
            
            ui.status.innerText = `🏆 ${names[winner]} WINS! 🏆`;
            speak(`${names[winner]} wins the round! Excellent play.`);
            fireMediumCrackers();
            return true;
        } else if (!boardData.includes("-")) {
            isGameActive = false;
            ui.status.innerText = "IT'S A DRAW!";
            speak("The match is a draw. No winner.");
            return true;
        }
        return false;
    }

    function updateTurnUI() {
        if (currentPlayer === 'X') {
            document.getElementById('cardP1').classList.add('active-turn');
            document.getElementById('cardP2').classList.remove('active-turn');
            ui.status.innerText = `${names.X}'s Turn`;
        } else {
            document.getElementById('cardP2').classList.add('active-turn');
            document.getElementById('cardP1').classList.remove('active-turn');
            ui.status.innerText = gameMode === 'pvsystem' ? "System Computing..." : `${names.O}'s Turn`;
        }
    }

    function resetRound() {
        boardData = ["-", "-", "-", "-", "-", "-", "-", "-", "-"];
        isGameActive = true; currentPlayer = 'X';
        buildBoard(); updateTurnUI();
        speak("Round reset.");
    }

    // --- MEDIUM SIZED CELEBRATION CRACKERS ---
    function fireMediumCrackers() {
        const duration = 2500;
        const end = Date.now() + duration;

        (function frame() {
            // Using scalar: 1.2 to make crackers Medium size
            confetti({ particleCount: 7, angle: 60, spread: 70, origin: { x: 0 }, scalar: 1.2, colors: ['#00f3ff', '#ff0055', '#39ff14'] });
            confetti({ particleCount: 7, angle: 120, spread: 70, origin: { x: 1 }, scalar: 1.2, colors: ['#00f3ff', '#ff0055', '#39ff14'] });

            if (Date.now() < end) requestAnimationFrame(frame);
        }());
    }
});