let enrollId = null;
let capturedCount = 0;
let isCollecting = false;

const mainCanvas = document.getElementById('mainCanvas');
const mainCtx = mainCanvas.getContext('2d');
const captureCanvas = document.getElementById('captureCanvas');
const captureCtx = captureCanvas.getContext('2d');

const maskLayer = document.getElementById('maskLayer');
const qualityBar = document.getElementById('qualityBar');
const qualityFill = document.getElementById('qualityFill');
const qualityText = document.getElementById('qualityText');

let fetchController = null;
let isFetching = false;
let currentImg = null;

window.onload = function() {
    const tempEmpData = JSON.parse(sessionStorage.getItem('tempEmployeeData') || '{}');
    if (tempEmpData.name) {
        document.getElementById('userName').value = tempEmpData.name;
        document.getElementById('loginUser').textContent = '当前采集：' + tempEmpData.name + '（工号：' + (tempEmpData.no || '未填写') + '）';
    }
    if (tempEmpData.no) document.getElementById('userId').value = tempEmpData.no;
    startFetching();
};

function updateStatus(message, type) {
    const el = document.getElementById('status-box');
    el.textContent = '状态：' + message;
    el.className = 'status-box ' + (type || 'info');
}

function updateProgress() {
    const percent = Math.round((capturedCount / 8) * 100);
    document.getElementById('progress-fill').style.width = percent + '%';
    document.getElementById('progress-fill').textContent = percent + '%';
    document.getElementById('progress-text').textContent = '已采集 ' + capturedCount + '/8 张';

    for (let i = 1; i <= 8; i++) {
        const thumb = document.getElementById('thumb-' + i);
        if (i <= capturedCount) {
            thumb.classList.add('captured');
            thumb.textContent = '&#10003;';
        } else {
            thumb.classList.remove('captured');
            thumb.textContent = '?';
        }
    }
}

async function startFetching() {
    if (isFetching) return;
    isFetching = true;
    async function loop() {
        while (isFetching) {
            await fetchFrameWithInfo();
            await new Promise(r => setTimeout(r, 100));
        }
    }
    loop();
}

async function fetchFrameWithInfo() {
    if (fetchController) fetchController.abort();
    fetchController = new AbortController();

    try {
        const response = await fetch(API_BASE_URL + '/face-video/frame-info', { signal: fetchController.signal });
        if (!response.ok) throw new Error('Fetch failed');
        const result = await response.json();
        if (result.code === 200) {
            renderFrame(result.data);
        }
    } catch (e) {
        if (e.name !== 'AbortError') console.error('Fetch frame error:', e);
    }
}

function renderFrame(data) {
    if (currentImg && !currentImg.complete) {
        return;
    }
    const img = new Image();
    currentImg = img;
    img.onload = () => {
        if (currentImg !== img) return;
        mainCtx.clearRect(0, 0, mainCanvas.width, mainCanvas.height);
        mainCtx.drawImage(img, 0, 0, mainCanvas.width, mainCanvas.height);

        if (data.hasFace && data.face) {
            mainCtx.strokeStyle = '#4caf50';
            mainCtx.lineWidth = 3;
            mainCtx.strokeRect(data.face.x, data.face.y, data.face.width, data.face.height);

            const quality = calculateQuality(data.face);
            showQualityBar(quality);
            hideMask();
        } else {
            showMask();
            hideQualityBar();
        }
    };
    img.src = 'data:image/jpeg;base64,' + data.jpegBase64;
}

function calculateQuality(face) {
    const canvasWidth = mainCanvas.width;
    const canvasHeight = mainCanvas.height;
    const faceCenterX = face.x + face.width / 2;
    const faceCenterY = face.y + face.height / 2;
    const faceArea = face.width * face.height;
    const canvasArea = canvasWidth * canvasHeight;

    const centerScore = 1 - (Math.abs(faceCenterX - canvasWidth / 2) / (canvasWidth / 2) + Math.abs(faceCenterY - canvasHeight / 2) / (canvasHeight / 2)) / 2;
    const sizeScore = Math.min(faceArea / (canvasArea * 0.15), 1);
    const quality = Math.round((centerScore * 0.4 + sizeScore * 0.6) * 100);
    return Math.min(100, Math.max(0, quality));
}

function showMask() {
    maskLayer.style.opacity = '1';
}

function hideMask() {
    maskLayer.style.opacity = '0';
}

function showQualityBar(quality) {
    qualityBar.style.opacity = '1';
    qualityFill.style.width = quality + '%';
    qualityText.textContent = '对正度: ' + quality + '%';
}

function hideQualityBar() {
    qualityBar.style.opacity = '0';
}

function stopFetching() {
    isFetching = false;
    if (fetchController) { fetchController.abort(); fetchController = null; }
}

function captureCurrentFrame() {
    return new Promise((resolve, reject) => {
        try {
            captureCanvas.width = mainCanvas.width;
            captureCanvas.height = mainCanvas.height;
            captureCtx.drawImage(mainCanvas, 0, 0, captureCanvas.width, captureCanvas.height);
            resolve(captureCanvas.toDataURL('image/jpeg', 0.85));
        } catch (e) {
            reject(e);
        }
    });
}

function startCollect() {
    const userId = document.getElementById('userId').value.trim();
    if (!userId) { alert('请输入用户编号'); return; }

    updateStatus('正在开始采集...', 'warning');

    fetch(API_BASE_URL + '/face/enroll/start', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ userId: userId, faceCount: 8 })
    })
    .then(res => res.json())
    .then(data => {
        if (data.code === 200) {
            enrollId = data.data.enrollId;
            capturedCount = 0;
            isCollecting = true;
            updateProgress();
            updateStatus('请对准摄像头，点击"拍照采集"', 'warning');
            document.getElementById('start-btn').disabled = true;
            document.getElementById('capture-btn').disabled = false;
            document.getElementById('complete-btn').disabled = true;
        } else {
            updateStatus(data.message || '启动失败', 'danger');
        }
    })
    .catch(err => updateStatus('网络错误', 'danger'));
}

function captureFace() {
    if (!enrollId || !isCollecting) return;

    const userId = document.getElementById('userId').value.trim();
    const captureBtn = document.getElementById('capture-btn');
    captureBtn.disabled = true;
    captureBtn.textContent = '处理中...';
    updateStatus('正在采集人脸...', 'warning');

    captureCurrentFrame()
        .then(imageData => {
            return fetch(API_BASE_URL + '/face/enroll/capture/image', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ enrollId: enrollId, userId: userId, image: imageData })
            });
        })
        .then(res => res.json())
        .then(data => {
            captureBtn.disabled = false;
            captureBtn.textContent = '拍照采集';

            if (data.code === 200) {
                const vo = data.data;
                capturedCount = vo.captured;
                updateProgress();

                if (vo.quality) {
                    updateStatus('已采集 ' + capturedCount + '/8 张 (质量: ' + (vo.quality * 100).toFixed(0) + '%)', 'warning');
                } else {
                    updateStatus(vo.message || '已采集 ' + capturedCount + '/8 张', 'warning');
                }

                if (capturedCount >= 8) {
                    document.getElementById('complete-btn').disabled = false;
                    captureBtn.disabled = true;
                }
            } else {
                updateStatus(data.data?.message || '采集失败', 'danger');
            }
        })
        .catch(err => {
            captureBtn.disabled = false;
            captureBtn.textContent = '拍照采集';
            updateStatus('采集失败', 'danger');
        });
}

function completeCollect() {
    if (!enrollId) { alert('请先点击"开始采集"启动录入流程'); return; }
    if (capturedCount < 8) { alert('还需要采集 ' + (8 - capturedCount) + ' 张人脸才能完成'); return; }

    const completeBtn = document.getElementById('complete-btn');
    completeBtn.disabled = true;
    completeBtn.textContent = '处理中...';
    updateStatus('正在完成采集...', 'info');

    fetch(API_BASE_URL + '/face/enroll/complete', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ enrollId: enrollId })
    })
    .then(res => res.json())
    .then(data => {
        completeBtn.disabled = false;
        completeBtn.textContent = '完成采集';

        if (data.code === 200) {
            updateStatus('人脸采集完成！', 'success');
            isCollecting = false;
            document.getElementById('capture-btn').disabled = true;
            completeBtn.disabled = true;
            document.getElementById('start-btn').disabled = false;
            sessionStorage.setItem('faceRegisterStatus', 'success');
            setTimeout(() => faceCollectSuccess(), 1500);
        } else {
            updateStatus(data.message || '完成失败', 'danger');
            completeBtn.disabled = false;
        }
    })
    .catch(err => {
        completeBtn.disabled = false;
        completeBtn.textContent = '完成采集';
        updateStatus('网络错误', 'danger');
    });
}

function faceCollectSuccess() {
    const tempEmpData = JSON.parse(sessionStorage.getItem('tempEmployeeData') || '{}');
    if (tempEmpData.fromAddPage) {
        window.location.href = '../admin/add_employee.html';
    } else {
        window.location.href = '../admin/admin_employee.html';
        sessionStorage.setItem('recentFaceRegisterEmpNo', tempEmpData.no);
    }
}

function backToPage() {
    stopFetching();

    fetch(API_BASE_URL + '/face-video/release', {
        method: 'GET',
        headers: { 'Authorization': 'Bearer ' + (getToken() || '') }
    }).catch(err => console.error('Release camera failed:', err));

    const tempEmpData = JSON.parse(sessionStorage.getItem('tempEmployeeData') || '{}');
    if (tempEmpData.fromAddPage) {
        window.location.href = '../admin/add_employee.html';
    } else {
        window.location.href = '../admin/admin_employee.html';
    }
}

window.onbeforeunload = function() {
    stopFetching();

    fetch(API_BASE_URL + '/face-video/release', {
        method: 'GET',
        headers: { 'Authorization': 'Bearer ' + (getToken() || '') }
    }).catch(err => console.error('Release camera failed:', err));
};