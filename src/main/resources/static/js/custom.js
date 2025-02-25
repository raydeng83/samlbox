// Upload SP
// document.getElementById('fileInputSp').addEventListener('change', function (e) {
//     const fileName = e.target.files[0]?.name || 'No file selected';
//     document.getElementById('fileNameSp').textContent = fileName;
// });

document.getElementById('uploadFormSp').addEventListener('submit', async function (e) {
    e.preventDefault();

    const fileInput = document.getElementById('fileInputSp');
    const file = fileInput.files[0];

    if (!file) {
        showError('Please select a file first');
        return;
    }

    const formData = new FormData();
    formData.append('file', file);

    try {
        const response = await fetch('/api/convert', {
            method: 'POST',
            body: formData
        });

        const result = await response.text();

        if (response.ok) {
            showResult(result);

        } else {
            showError(result);
        }
    } catch (error) {
        showError('An error occurred: ' + error.message);
    }
});

function showResult(content) {
    document.getElementById('resultSection').classList.remove('hidden');
    document.getElementById('errorAlert').classList.add('hidden');
    document.getElementById('resultContent').textContent = content;
}

function showError(message) {
    document.getElementById('errorAlert').classList.remove('hidden');
    document.getElementById('errorAlert').textContent = message;
    document.getElementById('resultSection').classList.add('hidden');
}

let dropZone = document.getElementById('dropZoneSp');
let fileInput = document.getElementById('fileInputSp');
let fileName = document.getElementById('file-nameSp');

// Handle file selection
fileInput.addEventListener('change', function (e) {
    handleFiles(this.files);
});

// Drag & Drop handlers
dropZone.addEventListener('dragover', (e) => {
    e.preventDefault();
    dropZone.classList.add('dragover');
});

dropZone.addEventListener('dragleave', () => {
    dropZone.classList.remove('dragover');
});

dropZone.addEventListener('drop', (e) => {
    e.preventDefault();
    dropZone.classList.remove('dragover');
    const files = e.dataTransfer.files;
    if (files.length) {
        fileInput.files = files;
        handleFiles(files);
    }
});

function handleFiles(files) {
    if (files.length > 0) {
        fileName.textContent = `Selected file: ${files[0].name}`;
    }
}







// Upload IDP
const dropZoneIdp = document.getElementById('dropZoneIdp');
const fileInputIdp = document.getElementById('fileInputIdp');
const fileNameIdp = document.getElementById('file-nameIdp');

// Handle file selection
fileInputIdp.addEventListener('change', function (e) {
    handleFilesIdp(this.files);
});

// Drag & Drop handlers
dropZoneIdp.addEventListener('dragover', (e) => {
    e.preventDefault();
    dropZoneIdp.classList.add('dragover');
});

dropZoneIdp.addEventListener('dragleave', () => {
    dropZoneIdp.classList.remove('dragover');
});

dropZoneIdp.addEventListener('drop', (e) => {
    e.preventDefault();
    dropZoneIdp.classList.remove('dragover');
    const files = e.dataTransfer.files;
    if (files.length) {
        fileInput.files = files;
        handleFiles(files);
    }
});

function handleFilesIdp(files) {
    if (files.length > 0) {
        fileNameIdp.textContent = `Selected file: ${files[0].name}`;
    }
}



