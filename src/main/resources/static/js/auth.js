let heartbeatTimer = null;
let lastActivityTime = Date.now();
let globalStream = null;

window.stopGlobalCamera = function() {
    if (globalStream) {
        globalStream.getTracks().forEach(track => track.stop());
        globalStream = null;
    }
};

function saveToken(token, userInfo) {
    sessionStorage.setItem(AUTH_TOKEN_KEY, token);
    sessionStorage.setItem(AUTH_USER_KEY, JSON.stringify(userInfo));
    lastActivityTime = Date.now();
}

function getToken() {
    return sessionStorage.getItem(AUTH_TOKEN_KEY);
}

function getUserInfo() {
    const userStr = sessionStorage.getItem(AUTH_USER_KEY);
    if (userStr) {
        try {
            return JSON.parse(userStr);
        } catch (e) {
            return null;
        }
    }
    return null;
}

function clearAuth() {
    sessionStorage.removeItem(AUTH_TOKEN_KEY);
    sessionStorage.removeItem(AUTH_USER_KEY);
    stopHeartbeat();
}

function checkAuth() {
    const token = getToken();
    if (!token) {
        return false;
    }
    return true;
}

function redirectToLogin() {
    clearAuth();
    window.location.href = '../index.html';
}

function logout() {
    if (typeof stopGlobalCamera === 'function') {
        stopGlobalCamera();
    }

    const token = getToken();
    if (token) {
        fetch(API_BASE_URL + '/auth/logout', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                [AUTH_HEADER]: token
            }
        }).catch(err => {
            console.error('Logout request failed:', err);
        }).finally(() => {
            clearAuth();
            redirectToLogin();
        });
    } else {
        redirectToLogin();
    }
}

function startHeartbeat() {
    if (heartbeatTimer) {
        return;
    }
    heartbeatTimer = setInterval(() => {
        sendHeartbeat();
    }, HEARTBEAT_INTERVAL);
}

function stopHeartbeat() {
    if (heartbeatTimer) {
        clearInterval(heartbeatTimer);
        heartbeatTimer = null;
    }
}

function sendHeartbeat() {
    const token = getToken();
    if (!token) {
        stopHeartbeat();
        return;
    }

    fetch(API_BASE_URL + '/auth/heartbeat', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            [AUTH_HEADER]: token
        }
    }).then(response => response.json())
      .then(data => {
          if (data.code === 401) {
              alert('会话已过期，请重新登录');
              redirectToLogin();
          } else {
              lastActivityTime = Date.now();
          }
      })
      .catch(err => {
          console.error('Heartbeat failed:', err);
      });
}

function recordActivity() {
    lastActivityTime = Date.now();
}

function checkSessionTimeout(timeoutMs) {
    if (!checkAuth()) {
        return false;
    }
    const elapsed = Date.now() - lastActivityTime;
    if (elapsed > timeoutMs) {
        alert('会话已超时，请重新登录');
        redirectToLogin();
        return false;
    }
    return true;
}

async function checkTokenValidity() {
    const token = getToken();
    if (!token) {
        return { valid: false, needReLogin: true, reason: 'No token' };
    }

    try {
        const response = await fetch(API_BASE_URL + '/auth/check', {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                [AUTH_HEADER]: token
            }
        });
        const data = await response.json();
        if (data.code === 200 && data.data) {
            return {
                valid: data.data.valid !== false,
                needReLogin: data.data.needReLogin === true,
                reason: data.data.reason || ''
            };
        }
        return { valid: false, needReLogin: true, reason: 'Token invalid' };
    } catch (err) {
        console.error('Check token validity failed:', err);
        return { valid: false, needReLogin: true, reason: 'Network error' };
    }
}

async function checkServerRestart() {
    const token = getToken();
    if (!token) {
        return false;
    }

    try {
        const response = await fetch(API_BASE_URL + '/auth/server-starttime', {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                [AUTH_HEADER]: token
            }
        });
        const data = await response.json();
        if (data.code === 200) {
            const serverStartTime = data.data;
            const userInfo = getUserInfo();
            if (userInfo && userInfo.createTime && serverStartTime > userInfo.createTime) {
                return true;
            }
        }
        return false;
    } catch (err) {
        console.error('Check server restart failed:', err);
        return false;
    }
}

function initPageAuth(timeoutMs) {
    if (!checkAuth()) {
        redirectToLogin();
        return false;
    }

    startHeartbeat();

    document.addEventListener('click', recordActivity);
    document.addEventListener('keypress', recordActivity);

    if (timeoutMs) {
        setInterval(() => {
            if (!checkSessionTimeout(timeoutMs)) {
                document.removeEventListener('click', recordActivity);
                document.removeEventListener('keypress', recordActivity);
            }
        }, 60000);
    }

    return true;
}

document.addEventListener('DOMContentLoaded', () => {
    document.addEventListener('click', recordActivity);
    document.addEventListener('keypress', recordActivity);
});