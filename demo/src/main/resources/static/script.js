const recordingButton = document.getElementById('recordingButton');
const statusText = document.getElementById('status');

let mediaRecorder;
let audioChunks = [];
let stream;

let recordingStatus = false;


recordingButton.addEventListener('click', async () =>{
    recordingStatus = !recordingStatus;

    if (recordingStatus) {
        stream = await navigator.mediaDevices.getUserMedia({
            audio: true
        });

        mediaRecorder = new MediaRecorder(stream);

        audioChunks = [];

        mediaRecorder.addEventListener("dataavailable", event =>{
            audioChunks.push(event.data);
        });

        mediaRecorder.addEventListener("stop", async () => {

            const audioBlob = new Blob(audioChunks, {
                type: "audio/webm"
            });

            console.log(audioBlob);
        });

        mediaRecorder.start();

        recordingButton.style.color = "red";
        recordingButton.textContent = "Stop Recording";


        statusText.style.color = "red";
        statusText.textContent= "Recording.....";
    } else {
        mediaRecorder.stop();

        stream.getTracks().forEach(track => {
            track.stop();
        });

        statusText.style.color = "green";
        statusText.textContent= "Ready";

        recordingButton.style.color = "green";
        recordingButton.textContent = "Start Recording";
    } 

});