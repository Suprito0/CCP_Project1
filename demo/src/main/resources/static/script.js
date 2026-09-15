const recordingButton = document.getElementById("recordingButton");
const statusText = document.getElementById("status");
const transcriptionText = document.getElementById("transcription");

let mediaRecorder;
let audioChunks = [];
let stream;

let recordingStatus = false;

recordingButton.addEventListener("click", async () => {

    if (!recordingStatus) {

        try {
            stream = await navigator.mediaDevices.getUserMedia({
                audio: true
            });

            mediaRecorder = new MediaRecorder(stream);

            audioChunks = [];

            mediaRecorder.addEventListener("dataavailable", event => {
                audioChunks.push(event.data);
            });

            mediaRecorder.addEventListener("stop", async () => {

                recordingButton.disabled = true;
                recordingButton.style.color = "grey";
                recordingButton.textContent = "Processing...";

                statusText.style.color = "grey";
                statusText.textContent = "Processing.....";

                const audioBlob = new Blob(audioChunks, {
                    type: "audio/webm"
                });

                const formData = new FormData();

                formData.append(
                    "audio",
                    audioBlob,
                    "recording.webm"
                );

                try {

                    const response = await fetch("/transcribe", {
                        method: "POST",
                        body: formData
                    });

                    if (!response.ok) {
                        throw new Error("Transcription failed");
                    }

                    const result = await response.text();

                    transcriptionText.textContent = result;

                    statusText.style.color = "green";
                    statusText.textContent = "Ready";

                } catch (error) {

                    statusText.style.color = "red";
                    statusText.textContent = "Error processing recording";

                    console.error(error);
                }

                recordingButton.disabled = false;
                recordingButton.style.color = "green";
                recordingButton.textContent = "Start Recording";
            });

            mediaRecorder.start();

            recordingStatus = true;

            recordingButton.style.color = "red";
            recordingButton.textContent = "Stop Recording";

            statusText.style.color = "red";
            statusText.textContent = "Recording.....";

        } catch (error) {

            statusText.style.color = "red";
            statusText.textContent = "Could not access microphone";

            console.error(error);
        }

    } else {

        recordingStatus = false;

        mediaRecorder.stop();

        stream.getTracks().forEach(track => {
            track.stop();
        });

        statusText.style.color = "grey";
        statusText.textContent = "Processing.....";

        recordingButton.disabled = true;
        recordingButton.style.color = "grey";
        recordingButton.textContent = "Processing...";
    }
});