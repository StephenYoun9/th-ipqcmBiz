globalStream = null;
let availableCameras = [];
let selectedCameraId = null;
const video = document.getElementById('video');
const canvas = document.getElementById('canvas');
const ctx = canvas.getContext('2d');

document.getElementById('pwdAuth').addEventListener('click', function () { switchAuth('pwd'); });
document.getElementById('fingerAuth').addEventListener('click', function () { switchAuth('finger'); });
document.getElementById('faceAuth').addEventListener('click', function () {
    switchAuth('face');
    detectCameras();
});

function switchAuth(type) {
    document.querySelectorAll('.auth-type button').forEach(btn => btn.classList.remove('active'));
    document.getElementById('pwdForm').style.display = 'none';
    document.getElementById('fingerForm').style.display = 'none';
    document.getElementById('faceForm').style.display = 'none';

    if (type !== 'face' && globalStream) {
        globalStream.getTracks().forEach(track => track.stop());
        globalStream = null;
        video.srcObject = null;
    }

    if (type === 'pwd') {
        document.getElementById('pwdAuth').classList.add('active');
        document.getElementById('pwdForm').style.display = 'block';
    } else if (type === 'finger') {
        document.getElementById('fingerAuth').classList.add('active');
        document.getElementById('fingerForm').style.display = 'block';
    } else if (type === 'face') {
        document.getElementById('faceAuth').classList.add('active');
        document.getElementById('faceForm').style.display = 'block';
    }
}

function detectCameras() {
    navigator.mediaDevices = navigator.mediaDevices || ((navigator.mozGetUserMedia || navigator.webkitGetUserMedia) ? {
        getUserMedia: function (c) {
            return new Promise(function (y, n) {
                (navigator.mozGetUserMedia || navigator.webkitGetUserMedia).call(navigator, c, y, n);
            });
        }
    } : null);

    if (!navigator.mediaDevices || !navigator.mediaDevices.enumerateDevices) {
        return;
    }

    navigator.mediaDevices.enumerateDevices().then(devices => {
        availableCameras = devices.filter(d => d.kind === 'videoinput');
        console.log('检测到摄像头数量:', availableCameras.length);
        console.log('摄像头列表:', availableCameras.map(d => d.label || 'camera-' + d.deviceId));

        const cameraSelect = document.getElementById('cameraSelect');
        if (availableCameras.length > 1) {
            cameraSelect.innerHTML = '<option value="">选择摄像头...</option>';
            availableCameras.forEach((cam, index) => {
                const label = cam.label || ('摄像头 ' + (index + 1));
                cameraSelect.innerHTML += `<option value="${cam.deviceId}">${label}</option>`;
            });
            cameraSelect.style.display = 'block';
        } else {
            cameraSelect.style.display = 'none';
            if (availableCameras.length === 1) {
                selectedCameraId = availableCameras[0].deviceId;
            }
        }

        initCamera();
    }).catch(err => {
        console.error('摄像头枚举失败:', err);
        initCamera();
    });
}

function releaseBackendCamera() {
    return fetch(API_BASE_URL + '/face-video/release', {
        method: 'GET'
    }).catch(err => console.warn('Release backend camera failed:', err));
}

function initCamera() {
    if (globalStream) {
        globalStream.getTracks().forEach(track => { track.stop(); track = null; });
        globalStream = null;
    }

    setTimeout(() => {
        releaseBackendCamera().then(() => {
            navigator.mediaDevices = navigator.mediaDevices || ((navigator.mozGetUserMedia || navigator.webkitGetUserMedia) ? {
                getUserMedia: function (c) {
                    return new Promise(function (y, n) {
                        (navigator.mozGetUserMedia || navigator.webkitGetUserMedia).call(navigator, c, y, n);
                    });
                }
            } : null);

            if (!navigator.mediaDevices) {
                alert('当前浏览器不支持摄像头调用，请使用Chrome/Firefox最新版！');
                return;
            }

            const constraints = {
                video: { width: {ideal: 640, max: 1280}, height: {ideal: 480, max: 720} },
                audio: false
            };

            const cameraSelect = document.getElementById('cameraSelect');
            if (cameraSelect && cameraSelect.value) {
                selectedCameraId = cameraSelect.value;
            }

            if (selectedCameraId) {
                constraints.video.deviceId = { exact: selectedCameraId };
            }

            navigator.mediaDevices.getUserMedia(constraints).then(mediaStream => {
                globalStream = mediaStream;
                video.srcObject = globalStream;
                video.onloadedmetadata = function () {
                    video.play().catch(err => console.warn('视频自动播放失败：', err));
                };
            }).catch(err => {
                console.error('摄像头初始化失败详情：', err);
                if (err.name === 'NotAllowedError') alert('摄像头权限被拒绝！请在浏览器地址栏左侧点击摄像头图标，允许本网站使用摄像头。');
                else if (err.name === 'NotFoundError') alert('未检测到摄像头设备！请检查摄像头是否连接或被其他程序占用。');
                else if (err.name === 'NotReadableError') alert('摄像头被占用！请关闭微信、钉钉、其他浏览器标签页等占用摄像头的程序后重试。');
                else alert('摄像头初始化失败：' + err.message + '\n错误类型：' + err.name);
            });
        });
    }, 100);
}

function login() {
    const userId = document.getElementById('loginUserId').value.trim();
    const password = document.getElementById('loginPassword').value;
    const rememberMe = document.getElementById('rememberMe').checked;

    if (!userId) { alert('请输入工号'); return; }
    if (!password) { alert('请输入密码'); return; }

    if (rememberMe) {
        localStorage.setItem('rememberedUserId', userId);
        localStorage.setItem('rememberedPassword', password);
    } else {
        localStorage.removeItem('rememberedUserId');
        localStorage.removeItem('rememberedPassword');
    }

    fetch(API_BASE_URL + '/login/pwd', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({userId, password})
    })
        .then(response => response.json())
        .then(data => {
            if (data.code === 200 && data.data) {
                const token = data.data.token;
                const userInfo = data.data.userInfo;
                saveToken(token, userInfo);
                showSuccessModal(userInfo);
            } else {
                alert('登录失败，原因：' + (data.message || '未知错误'));
            }
        })
        .catch(error => {
            console.error('登录请求失败：', error);
            alert('网络异常，请重试');
        });
}

function fingerLogin() {
    const fingerprint = 'finger_' + new Date().getTime();
    fetch(API_BASE_URL + '/login/finger', {
        method: 'POST',
        headers: {'Content-Type': 'application/x-www-form-urlencoded'},
        body: 'fingerprint=' + fingerprint
    })
        .then(response => response.json())
        .then(data => {
            if (data.code === 200 && data.data) {
                const token = data.data.token;
                const userInfo = data.data.userInfo;
                saveToken(token, userInfo);
                showSuccessModal(userInfo);
            } else {
                alert(data.message || '登录失败');
            }
        })
        .catch(error => {
            console.error('指纹认证失败：', error);
            alert('网络异常，请重试');
        });
}

function startFaceRecognition() {
    const btn = document.getElementById('face-login-btn');
    const statusMsg = document.getElementById('face-status-msg');
    const statusOverlay = document.getElementById('face-status-overlay');
    const statusText = document.getElementById('face-status-text');

    btn.disabled = true;
    btn.textContent = '正在识别...';
    statusMsg.textContent = '正在采集人脸，请保持正脸对着摄像头...';
    statusMsg.className = 'face-status-msg status-info';
    statusOverlay.style.display = 'flex';
    statusText.textContent = '正在识别...';

    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    ctx.drawImage(video, 0, 0);

    const faceImageBase64 = canvas.toDataURL('image/jpeg', 0.8).replace('data:image/jpeg;base64,', '');

    fetch(API_BASE_URL + '/login/face', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({faceImageBase64: faceImageBase64})
    })
        .then(response => response.json())
        .then(data => {
            statusOverlay.style.display = 'none';
            btn.disabled = false;
            btn.textContent = '开始人脸认证';
            if (data.code === 200 && data.data) {
                const token = data.data.token;
                const userInfo = data.data.userInfo;
                saveToken(token, userInfo);
                showSuccessModal(userInfo);
            } else {
                statusMsg.textContent = '认证失败：' + (data.message || '未知错误');
                statusMsg.className = 'face-status-msg status-error';
                setTimeout(() => { statusMsg.textContent = ''; }, 3000);
            }
        })
        .catch(error => {
            console.error('人脸认证请求失败：', error);
            statusOverlay.style.display = 'none';
            btn.disabled = false;
            btn.textContent = '开始人脸认证';
            statusMsg.textContent = '网络异常，请重试';
            statusMsg.className = 'face-status-msg status-error';
            setTimeout(() => { statusMsg.textContent = ''; }, 3000);
        });
}

function saveToken(token, userInfo) {
    sessionStorage.setItem(AUTH_TOKEN_KEY, token);
    const userData = {
        ...userInfo,
        createTime: Date.now()
    };
    sessionStorage.setItem(AUTH_USER_KEY, JSON.stringify(userData));
}

window.onbeforeunload = function () {
    if (globalStream) globalStream.getTracks().forEach(track => track.stop());
};

function showSuccessModal(userData) {
    const modal = document.getElementById('success-modal');
    const userNameEl = document.getElementById('success-user-name');
    const userRoleEl = document.getElementById('success-user-role');

    userNameEl.textContent = userData.userName || '-';
    userRoleEl.textContent = userData.userRole === 0 ? '管理员' : '普通员工';

    modal.style.display = 'flex';

    setTimeout(() => {
        window.location.href = userData.userRole === 0 ? 'admin/admin_index.html' : 'employee/employee_index.html';
    }, 3000);
}

window.onload = function () {
    document.querySelector('#fingerForm .btn').onclick = fingerLogin;

    const cameraSelect = document.getElementById('cameraSelect');
    if (cameraSelect) {
        cameraSelect.addEventListener('change', function() {
            if (this.value) {
                selectedCameraId = this.value;
                initCamera();
            }
        });
    }

    const rememberedUserId = localStorage.getItem('rememberedUserId');
    const rememberedPassword = localStorage.getItem('rememberedPassword');
    if (rememberedUserId) {
        document.getElementById('loginUserId').value = rememberedUserId;
    }
    if (rememberedPassword) {
        document.getElementById('loginPassword').value = rememberedPassword;
        document.getElementById('rememberMe').checked = true;
    }
};

function showForgotPassword() {
    document.getElementById('forgot-modal').style.display = 'flex';
}

function closeForgotModal() {
    document.getElementById('forgot-modal').style.display = 'none';
}