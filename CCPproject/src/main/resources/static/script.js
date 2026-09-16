// Cache the page elements that JavaScript updates while recording/transcribing.
// Used AI to help with the error handling here
const recordingButton = document.getElementById("recordingButton");
const statusText = document.getElementById("status");
const transcriptionText = document.getElementById("transcription");
const recordingIndicator = document.getElementById("recordingIndicator");
const timerText = document.getElementById("timer");

// Client-side limits keep recordings short and prevent a request from hanging forever.
const MAX_RECORDING_MS = 55_000;
const REQUEST_TIMEOUT_MS = 15_000;

// Mutable state for the current browser recording session.
let mediaRecorder = null;
let mediaStream = null;
let audioChunks = [];
let isRecording = false;
let autoStopTimer = null;
let elapsedTimer = null;
let recordingStartedAt = 0;

// Updates the visible status text and a data attribute used by CSS for styling.
function setStatus(message, kind = "ready") {
  statusText.textContent = message;
  statusText.dataset.kind = kind;
}

// These helpers keep button text, disabled state and ARIA state consistent.
function setButtonReady() {
  recordingButton.disabled = false;
  recordingButton.textContent = "Start Recording";
  recordingButton.dataset.state = "ready";
  recordingButton.setAttribute("aria-pressed", "false");
}

function setButtonRecording() {
  recordingButton.disabled = false;
  recordingButton.textContent = "Stop Recording";
  recordingButton.dataset.state = "recording";
  recordingButton.setAttribute("aria-pressed", "true");
}

function setButtonBusy() {
  recordingButton.disabled = true;
  recordingButton.textContent = "Transcribing…";
  recordingButton.dataset.state = "busy";
  recordingButton.setAttribute("aria-pressed", "false");
}

// Displays elapsed recording time as mm:ss.
function updateTimer() {
  const elapsedSeconds = Math.floor((Date.now() - recordingStartedAt) / 1000);
  const minutes = String(Math.floor(elapsedSeconds / 60)).padStart(2, "0");
  const seconds = String(elapsedSeconds % 60).padStart(2, "0");
  timerText.textContent = `${minutes}:${seconds}`;
}

// Releasing microphone tracks turns off the browser's microphone capture promptly.
function stopTracks() {
  if (mediaStream) {
    mediaStream.getTracks().forEach((track) => track.stop());
    mediaStream = null;
  }
}

function clearRecordingTimers() {
  window.clearTimeout(autoStopTimer);
  window.clearInterval(elapsedTimer);
  autoStopTimer = null;
  elapsedTimer = null;
}

// Choose an efficient speech format supported by the current browser.
function chooseRecorderOptions() {
  // A modest bitrate reduces upload size/latency while remaining appropriate for speech.
  const options = { audioBitsPerSecond: 32_000 };

  if (MediaRecorder.isTypeSupported("audio/webm;codecs=opus")) {
    options.mimeType = "audio/webm;codecs=opus";
  } else if (MediaRecorder.isTypeSupported("audio/ogg;codecs=opus")) {
    options.mimeType = "audio/ogg;codecs=opus";
  }

  return options;
}

// Requests microphone permission, creates MediaRecorder and begins collecting chunks.
async function startRecording() {
  if (
    !navigator.mediaDevices?.getUserMedia ||
    typeof MediaRecorder === "undefined"
  ) {
    setStatus("This browser does not support microphone recording.", "error");
    recordingButton.disabled = true;
    return;
  }

  try {
    mediaStream = await navigator.mediaDevices.getUserMedia({ audio: true });
    mediaRecorder = new MediaRecorder(mediaStream, chooseRecorderOptions());
    audioChunks = [];

    mediaRecorder.addEventListener("dataavailable", (event) => {
      if (event.data && event.data.size > 0) {
        audioChunks.push(event.data);
      }
    });

    mediaRecorder.addEventListener("stop", uploadRecording, { once: true });

    // Ask MediaRecorder for one-second chunks instead of holding one monolithic buffer.
    mediaRecorder.start(1000);
    isRecording = true;
    recordingStartedAt = Date.now();
    timerText.textContent = "00:00";
    elapsedTimer = window.setInterval(updateTimer, 250);
    autoStopTimer = window.setTimeout(
      () => stopRecording(true),
      MAX_RECORDING_MS,
    );

    recordingIndicator.classList.add("recording");
    setButtonRecording();
    setStatus("Recording — speak now.", "recording");
  } catch (error) {
    console.error(error);
    stopTracks();
    setButtonReady();
    setStatus("Microphone access was denied or unavailable.", "error");
  }
}

// Stops capture and switches the UI into the processing state.
function stopRecording(automatic = false) {
  if (!isRecording || !mediaRecorder) {
    return;
  }

  isRecording = false;
  clearRecordingTimers();
  recordingIndicator.classList.remove("recording");
  setButtonBusy();
  setStatus(
    automatic
      ? "Maximum recording length reached. Processing…"
      : "Processing recording…",
    "busy",
  );

  if (mediaRecorder.state !== "inactive") {
    mediaRecorder.stop();
  }

  stopTracks();
}

// Combines recorded chunks into one Blob and sends it to the Spring endpoint.
async function uploadRecording() {
  try {
    let mimeType = mediaRecorder?.mimeType || "audio/webm";
    mimeType = mimeType.split(";")[0].trim();

    if (audioChunks.length === 0) {
      throw new Error("No audio was captured.");
    }

    const audioBlob = new Blob(audioChunks, { type: mimeType });
    const extension = mimeType.includes("ogg") ? "ogg" : "webm";
    const formData = new FormData();
    formData.append("audio", audioBlob, `recording.${extension}`);

    audioChunks = [];
    setStatus("Transcribing…", "busy");

    const controller = new AbortController();
    const timeout = window.setTimeout(
      () => controller.abort(),
      REQUEST_TIMEOUT_MS,
    );

    let response;
    try {
      response = await fetch("/api/v1/transcribe", {
        method: "POST",
        body: formData,
        signal: controller.signal,
      });
    } finally {
      window.clearTimeout(timeout);
    }

    const result = await response.text();

    if (!response.ok) {
      throw new Error(result || `Server returned HTTP ${response.status}`);
    }

    transcriptionText.textContent = result;
    timerText.textContent = "00:00";
    setStatus("Ready", "ready");
    setButtonReady();
  } catch (error) {
    console.error(error);
    timerText.textContent = "00:00";
    setButtonReady();

    if (error.name === "AbortError") {
      setStatus("Transcription timed out. Please try again.", "error");
    } else {
      setStatus(error.message || "Error processing recording.", "error");
    }
  } finally {
    mediaRecorder = null;
  }
}

// One button toggles between starting and stopping the current recording.
recordingButton.addEventListener("click", () => {
  if (isRecording) {
    stopRecording(false);
  } else {
    startRecording();
  }
});

setButtonReady();
