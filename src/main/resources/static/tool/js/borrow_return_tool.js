// 状态管理
let currentDetectedTools = [];
let initialInventory = [];
let currentInventory = [];
let cameraRunning = false;
let frameTimer = null;
let cabinetInitialized = false;
let operationStarted = false;

// 获取工具中文名称
function getToolChineseName(toolCodeOrLabel) {
    const nameMap = {
        'wrench': '扳手',
        'screwdriver': '螺丝刀',
        'pliers': '钳子'
    };
    const label = toolCodeOrLabel.split('-')[0];
    return nameMap[label] || toolCodeOrLabel;
}

// 打开柜门
async function openDoor() {
    if (!cabinetInitialized) {
        alert('请先初始化柜中工具');
        return;
    }

    const token = sessionStorage.getItem(AUTH_TOKEN_KEY);
    if (!token) {
        alert('用户未登录，请先登录');
        return;
    }

    try {
        // 1. 获取当前库存作为初始状态（只获取在柜中的工具，用于借还计算）
        const invResponse = await fetch(API_BASE_URL + '/tool/cabinet/inventory?statusOnly=true', {
            method: 'GET',
            headers: { [AUTH_HEADER]: token }
        });
        const invData = await invResponse.json();
        if (invData.code === 200) {
            initialInventory = invData.data || [];
        }

        // 2. 调用后端打开柜门
        const response = await fetch(API_BASE_URL + '/tool/cabinet/open-door', {
            method: 'POST',
            headers: { [AUTH_HEADER]: token }
        });
        const data = await response.json();

        if (data.code !== 200) {
            alert('打开柜门失败: ' + (data.message || '未知错误'));
            return;
        }

        // 3. 启动摄像头
        const cameraStarted = await startCamera();
        if (!cameraStarted) {
            alert('摄像头启动失败');
            return;
        }

        operationStarted = true;
        document.getElementById('btnOpenDoor').style.display = 'none';
        document.getElementById('btnCompleteOp').style.display = 'inline-block';
        document.getElementById('btnCompleteOp').disabled = false;
        document.getElementById('btnCancel').style.display = 'inline-block';
        document.getElementById('doorStatus').textContent = '柜门已打开';

        updateOperationLog('柜门已打开，请拿取所需工具或放入归还的工具');

    } catch (err) {
        console.error('打开柜门失败:', err);
        alert('打开柜门失败，请稍后重试');
    }
}

// 启动摄像头
async function startCamera() {
    try {
        const response = await fetch(API_BASE_URL + '/tool/cabinet/camera/start', {
            method: 'POST',
            headers: { [AUTH_HEADER]: sessionStorage.getItem(AUTH_TOKEN_KEY) }
        });
        const data = await response.json();

        if (data.code === 200) {
            cameraRunning = true;
            document.getElementById('videoContainer').style.display = 'block';
            startFramePolling();
            return true;
        } else {
            alert('摄像头启动失败: ' + (data.message || '未知错误'));
            return false;
        }
    } catch (err) {
        console.error('摄像头启动失败:', err);
        alert('摄像头启动失败，请检查摄像头连接');
        return false;
    }
}

// 停止摄像头
async function stopCamera() {
    if (frameTimer) {
        clearInterval(frameTimer);
        frameTimer = null;
    }

    try {
        await fetch(API_BASE_URL + '/tool/cabinet/camera/release', {
            method: 'POST',
            headers: { [AUTH_HEADER]: sessionStorage.getItem(AUTH_TOKEN_KEY) }
        });
    } catch (err) {
        console.error('摄像头释放失败:', err);
    }

    cameraRunning = false;
    document.getElementById('videoContainer').style.display = 'none';
}

// 轮询获取视频帧
function startFramePolling() {
    frameTimer = setInterval(async () => {
        if (!cameraRunning) {
            clearInterval(frameTimer);
            return;
        }

        const img = document.getElementById('videoFrame');
        img.src = API_BASE_URL + '/tool-recognition/frame?t=' + Date.now();

        try {
            const response = await fetch(API_BASE_URL + '/tool/cabinet/frame-info', {
                method: 'GET',
                headers: { [AUTH_HEADER]: sessionStorage.getItem(AUTH_TOKEN_KEY) }
            });
            const data = await response.json();

            if (data.code === 200 && data.data) {
                updateDetectionDisplay(data.data);
            }
        } catch (err) {
            console.error('获取识别结果失败:', err);
        }
    }, 300);
}

// 更新检测结果显示
function updateDetectionDisplay(data) {
    currentDetectedTools = data.detectedTools || [];

    const listEl = document.getElementById('detectionList');
    const statusEl = document.getElementById('recognitionStatus');

    if (currentDetectedTools.length === 0) {
        listEl.innerHTML = '<li>暂无检测结果</li>';
        statusEl.textContent = '未检测到工具';
    } else {
        listEl.innerHTML = currentDetectedTools.map(tool =>
            '<li>' + getToolChineseName(tool) + ' (' + tool + ')</li>'
        ).join('');
        statusEl.textContent = '检测到 ' + currentDetectedTools.length + ' 种工具';
    }
}

// 完成操作
async function completeOperation() {
    if (!operationStarted) {
        return;
    }

    // 停止摄像头
    await stopCamera();

    // 获取最终检测结果
    let finalTools = [...currentDetectedTools];

    // 计算借了和还了
    const initialSet = new Set(initialInventory.map(inv => inv.toolCode));
    const finalSet = new Set(finalTools);

    // 借走的 = 初始有但最终没有的
    const borrowed = [];
    initialSet.forEach(tool => {
        if (!finalSet.has(tool)) {
            borrowed.push(tool);
        }
    });

    // 还了的 = 最终有但初始没有的
    const returned = [];
    finalSet.forEach(tool => {
        if (!initialSet.has(tool)) {
            returned.push(tool);
        }
    });

    // 显示结果
    document.getElementById('initialToolsDisplay').textContent =
        Array.from(initialSet).map(t => getToolChineseName(t)).join(', ') || '无';
    document.getElementById('currentToolsDisplay').textContent =
        finalTools.map(t => getToolChineseName(t)).join(', ') || '无';
    document.getElementById('borrowedDisplay').textContent =
        borrowed.length > 0 ? borrowed.map(t => getToolChineseName(t)).join(', ') : '无';
    document.getElementById('returnedDisplay').textContent =
        returned.length > 0 ? returned.map(t => getToolChineseName(t)).join(', ') : '无';

    document.getElementById('resultPanel').style.display = 'block';
    document.getElementById('btnCompleteOp').disabled = true;
    document.getElementById('doorStatus').textContent = '待确认';

    updateOperationLog('检测完成，请确认操作结果');
}

// 确认结果
async function confirmResult() {
    const initialSet = new Set(initialInventory.map(inv => inv.toolCode));
    const finalSet = new Set(currentDetectedTools);

    const borrowed = [];
    initialSet.forEach(tool => {
        if (!finalSet.has(tool)) {
            borrowed.push(tool);
        }
    });

    const returned = [];
    finalSet.forEach(tool => {
        if (!initialSet.has(tool)) {
            returned.push(tool);
        }
    });

    try {
        const response = await fetch(API_BASE_URL + '/tool/cabinet/process-borrow-return', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                [AUTH_HEADER]: sessionStorage.getItem(AUTH_TOKEN_KEY)
            },
            body: JSON.stringify({
                initialTools: Array.from(initialSet),
                finalTools: currentDetectedTools
            })
        });

        const data = await response.json();

        if (data.code === 200) {
            const result = data.data;
            const borrowedNames = result.borrowed.map(t => getToolChineseName(t)).join(', ');
            const returnedNames = result.returned.map(t => getToolChineseName(t)).join(', ');
            updateOperationLog('操作完成！借了' + result.borrowedCount + '件，还了' + result.returnedCount + '件');
            alert('操作完成！借了' + result.borrowedCount + '件，还了' + result.returnedCount + '件');
            resetState();
            loadInventory();
        } else {
            alert('操作失败: ' + (data.message || '未知错误'));
        }
    } catch (err) {
        console.error('确认结果失败:', err);
        alert('操作失败，请稍后重试');
    }
}

// 重新识别
async function reDetect() {
    document.getElementById('resultPanel').style.display = 'none';
    document.getElementById('btnCompleteOp').disabled = false;
    document.getElementById('doorStatus').textContent = '柜门已打开';

    const cameraStarted = await startCamera();
    if (!cameraStarted) {
        alert('摄像头启动失败');
    }
}

// 取消操作
async function cancelOperation() {
    await stopCamera();
    resetState();
}

// 重置状态
function resetState() {
    operationStarted = false;
    currentDetectedTools = [];
    initialInventory = [];
    currentInventory = [];

    document.getElementById('btnOpenDoor').style.display = 'inline-block';
    document.getElementById('btnOpenDoor').disabled = false;
    document.getElementById('btnCompleteOp').style.display = 'none';
    document.getElementById('btnCompleteOp').disabled = true;
    document.getElementById('btnCancel').style.display = 'none';
    document.getElementById('doorStatus').textContent = '准备就绪';
    document.getElementById('resultPanel').style.display = 'none';
    document.getElementById('videoContainer').style.display = 'none';
}

// 加载柜中库存列表
async function loadInventory() {
    try {
        const response = await fetch(API_BASE_URL + '/tool/cabinet/inventory', {
            method: 'GET',
            headers: { [AUTH_HEADER]: sessionStorage.getItem(AUTH_TOKEN_KEY) }
        });
        const data = await response.json();

        if (data.code === 200) {
            const inventoryList = data.data || [];
            const tbody = document.getElementById('inventoryTableBody');
            tbody.innerHTML = '';

            if (inventoryList.length === 0) {
                tbody.innerHTML = '<tr><td colspan="4" style="text-align:center;padding:8px;">暂无工具</td></tr>';
            } else {
                inventoryList.forEach(item => {
                    const tr = document.createElement('tr');
                    const statusMap = { '0': '在柜中', '1': '已借出', '2': '维护中' };
                    const statusText = statusMap[item.status] || item.status;
                    const statusColor = item.status === '0' ? '#2ecc71' : item.status === '1' ? '#e74c3c' : '#ffa500';
                    const toolName = getToolChineseName(item.toolCode);
                    tr.innerHTML = `
                        <td style="padding: 8px; border: 1px solid #ddd; text-align: center;">${item.position}</td>
                        <td style="padding: 8px; border: 1px solid #ddd; text-align: center;">${item.toolCode}</td>
                        <td style="padding: 8px; border: 1px solid #ddd; text-align: center;">${toolName}</td>
                        <td style="padding: 8px; border: 1px solid #ddd; text-align: center; color: ${statusColor};">${statusText}</td>
                    `;
                    tbody.appendChild(tr);
                });
            }

            document.getElementById('inventoryPanel').style.display = 'block';
        }
    } catch (err) {
        console.error('加载库存失败:', err);
    }
}

// 更新操作日志
function updateOperationLog(message) {
    const logEl = document.getElementById('operationLog');
    const timestamp = new Date().toLocaleTimeString();
    logEl.innerHTML = '[' + timestamp + '] ' + message;
}

// 页面加载时检查柜子是否已初始化
window.onload = async function() {
    try {
        const response = await fetch(API_BASE_URL + '/tool/cabinet/inventory', {
            method: 'GET',
            headers: { [AUTH_HEADER]: sessionStorage.getItem(AUTH_TOKEN_KEY) }
        });
        const data = await response.json();

        if (data.code === 200 && data.data && data.data.length > 0) {
            cabinetInitialized = true;
            const initStatusEl = document.getElementById('initStatus');
            if (initStatusEl) {
                initStatusEl.textContent = '已初始化 - ' + data.data.length + '个位置';
                initStatusEl.style.color = '#2ecc71';
            }
            loadInventory();
            updateOperationLog('柜子已初始化，共' + data.data.length + '个位置，可以开始借还工具');
        } else {
            cabinetInitialized = false;
            const initStatusEl = document.getElementById('initStatus');
            if (initStatusEl) {
                initStatusEl.textContent = '柜子未初始化，请联系管理员';
                initStatusEl.style.color = '#e74c3c';
            }
            updateOperationLog('柜子未初始化，请联系管理员进行初始化');
        }
    } catch (err) {
        console.error('检查柜子状态失败:', err);
    }
};

// 退出登录
function logout() {
    if (confirm('确定要退出登录吗？')) {
        sessionStorage.clear();
        window.location.href = '../index.html';
    }
}