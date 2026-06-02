let toolList = [];
let currentEditTool = null;
let selectedImageFile = null;
let existingImageUrl = null;
let toolPageNum = 1;
let toolTotal = 0;
let cameraRunning = false;
let frameTimer = null;
let cabinetInitialized = false;

function openAddToolModal() {
    currentEditTool = null;
    selectedImageFile = null;
    existingImageUrl = null;
    document.getElementById('toolName').value = '';
    document.getElementById('toolCode').value = '';
    document.getElementById('cabinetNo').value = 'A01';
    document.getElementById('toolType').value = 'hand';
    document.querySelector('#addToolModal h3').textContent = '上架新工具';
    document.getElementById('statusGroup').style.display = 'none';
    document.getElementById('toolCode').readOnly = false;
    document.getElementById('toolImage').value = '';
    document.getElementById('imageName').textContent = '';
    document.getElementById('imagePreview').style.display = 'none';
    document.getElementById('addToolModal').style.display = 'flex';
}

function openEditToolModal(tool) {
    currentEditTool = tool;
    selectedImageFile = null;
    existingImageUrl = tool.imageUrl;
    document.querySelector('#addToolModal h3').textContent = '编辑工具';
    document.getElementById('toolName').value = tool.toolName;
    document.getElementById('toolCode').value = tool.toolCode;
    document.getElementById('toolCode').readOnly = true;
    document.getElementById('cabinetNo').value = tool.cabinetNo;
    document.getElementById('toolType').value = tool.toolType;
    document.getElementById('toolStatus').value = tool.status;
    document.getElementById('statusGroup').style.display = 'block';
    document.getElementById('toolImage').value = '';
    document.getElementById('imageName').textContent = '';

    if (tool.imageUrl) {
        document.getElementById('previewImg').src = API_BASE_URL + tool.imageUrl;
        document.getElementById('imagePreview').style.display = 'block';
    } else {
        document.getElementById('imagePreview').style.display = 'none';
    }

    document.getElementById('addToolModal').style.display = 'flex';
}

function closeAddToolModal() {
    document.getElementById('addToolModal').style.display = 'none';
    document.getElementById('toolCode').readOnly = false;
    document.getElementById('statusGroup').style.display = 'none';
    selectedImageFile = null;
    existingImageUrl = null;
}

function handleImageSelect(input) {
    if (input.files && input.files[0]) {
        selectedImageFile = input.files[0];
        document.getElementById('imageName').textContent = selectedImageFile.name;

        const reader = new FileReader();
        reader.onload = function(e) {
            document.getElementById('previewImg').src = e.target.result;
            document.getElementById('imagePreview').style.display = 'block';
        };
        reader.readAsDataURL(selectedImageFile);
    }
}

function removeImage() {
    selectedImageFile = null;
    document.getElementById('toolImage').value = '';
    document.getElementById('imageName').textContent = '';

    if (existingImageUrl) {
        document.getElementById('previewImg').src = API_BASE_URL + existingImageUrl;
        document.getElementById('imagePreview').style.display = 'block';
    } else {
        document.getElementById('imagePreview').style.display = 'none';
    }
}

function loadToolList(keyword, pageNum) {
    if (pageNum) toolPageNum = pageNum;
    const token = sessionStorage.getItem(AUTH_TOKEN_KEY);
    const pageSize = 15;
    fetch(API_BASE_URL + '/admin/tool/list/paged?keyword=' + encodeURIComponent(keyword || '') + '&pageNum=' + toolPageNum + '&pageSize=' + pageSize, {
        headers: { [AUTH_HEADER]: token }
    })
    .then(response => response.json())
    .then(data => {
        if (data.code === 200 && data.data) {
            toolList = data.data.list || [];
            toolTotal = data.data.total;
            renderToolTable();
            renderToolPagination();
        }
    });
}

function renderToolPagination() {
    const container = document.getElementById('paginationContainer');
    const totalPages = Math.ceil(toolTotal / 15);
    container.innerHTML = `
        <button class="btn btn-default btn-sm" onclick="loadToolList(document.getElementById('searchKeyword').value, ${toolPageNum - 1})" ${toolPageNum <= 1 ? 'disabled' : ''}>上一页</button>
        <span style="margin: 0 5px;">第</span>
        <input type="number" id="toolPageInput" value="${toolPageNum}" min="1" max="${totalPages}" style="width: 50px; text-align: center; padding: 2px;" onkeypress="if(event.key==='Enter'){let p=parseInt(this.value);if(p>=1&&p<=${totalPages})loadToolList(document.getElementById('searchKeyword').value, p);}">
        <span style="margin: 0 5px;">/ ${totalPages} 页，共 ${toolTotal} 条</span>
        <button class="btn btn-default btn-sm" onclick="loadToolList(document.getElementById('searchKeyword').value, ${toolPageNum + 1})" ${toolPageNum >= totalPages ? 'disabled' : ''}>下一页</button>
    `;
}

function renderToolTable() {
    const tbody = document.getElementById('toolTableBody');
    if (!tbody) return;

    if (toolList.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;">暂无数据</td></tr>';
        return;
    }

    tbody.innerHTML = toolList.map(tool => {
        const statusMap = {
            'AVAILABLE': '可借',
            'BORROWED': '已借出',
            'MAINTENANCE': '维护中',
            'OFFLINE': '已下架'
        };
        const statusText = statusMap[tool.status] || tool.status;
        const statusClass = tool.status === 'AVAILABLE' ? 'status-available' :
                           tool.status === 'MAINTENANCE' ? 'status-maintenance' : '';

        let actionButtons = '';
        if (tool.status === 'MAINTENANCE') {
            actionButtons = `<button class="btn btn-success btn-sm" onclick="finishMaintenance('${tool.toolCode}')">完成维护</button>`;
        } else if (tool.status !== 'OFFLINE') {
            actionButtons = `<button class="btn btn-warning btn-sm" onclick="startMaintenance('${tool.toolCode}')">维护</button>`;
        }

        return `<tr>
            <td>${tool.toolName}</td>
            <td>${tool.toolCode}</td>
            <td>${tool.cabinetNo}</td>
            <td>${getToolTypeName(tool.toolType)}</td>
            <td><span class="status-badge ${statusClass}">${statusText}</span></td>
            <td>
                <button class="btn btn-default btn-sm" onclick="openEditToolModal(toolList.find(t=>t.toolCode==='${tool.toolCode}'))">编辑</button>
                <button class="btn btn-danger btn-sm" onclick="offlineTool('${tool.toolCode}')">下架</button>
                ${actionButtons}
            </td>
        </tr>`;
    }).join('');
}

function getToolTypeName(type) {
    const typeMap = {
        'hand': '手动工具',
        'electric': '电动工具',
        'measuring': '测量工具'
    };
    return typeMap[type] || type;
}

function saveTool() {
    const token = sessionStorage.getItem(AUTH_TOKEN_KEY);
    const isEdit = currentEditTool !== null;
    const method = isEdit ? 'PUT' : 'POST';
    const url = isEdit ? API_BASE_URL + '/admin/tool' : API_BASE_URL + '/admin/tool';

    const toolCode = document.getElementById('toolCode').value;
    const toolName = document.getElementById('toolName').value;
    const cabinetNo = document.getElementById('cabinetNo').value;
    const toolType = document.getElementById('toolType').value;
    const status = document.getElementById('toolStatus').value;

    if (!toolName || !toolCode) {
        alert('请填写必填项');
        return;
    }

    const data = {
        toolCode: toolCode,
        toolName: toolName,
        cabinetNo: cabinetNo,
        toolType: toolType,
        status: status,
        imageUrl: existingImageUrl
    };

    fetch(url, {
        method: method,
        headers: {
            [AUTH_HEADER]: token,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(data)
    })
    .then(response => response.json())
    .then(data => {
        if (data.code === 200) {
            if (selectedImageFile) {
                uploadImage(toolCode, isEdit);
            } else {
                alert(isEdit ? '更新成功' : '上架成功');
                closeAddToolModal();
                loadToolList(document.getElementById('searchKeyword').value);
            }
        } else {
            alert(data.message || '操作失败');
        }
    });
}

function uploadImage(toolCode, isEdit) {
    const token = sessionStorage.getItem(AUTH_TOKEN_KEY);
    const formData = new FormData();
    formData.append('file', selectedImageFile);

    fetch(API_BASE_URL + '/admin/tool/image/' + toolCode, {
        method: 'POST',
        headers: {
            [AUTH_HEADER]: token
        },
        body: formData
    })
    .then(response => response.json())
    .then(data => {
        if (data.code === 200) {
            existingImageUrl = data.data;
            alert(isEdit ? '更新成功' : '上架成功');
            closeAddToolModal();
            loadToolList(document.getElementById('searchKeyword').value);
        } else {
            alert((isEdit ? '更新成功但' : '上架成功但') + '图片上传失败: ' + data.message);
            closeAddToolModal();
            loadToolList(document.getElementById('searchKeyword').value);
        }
    })
    .catch(err => {
        alert(isEdit ? '更新成功但图片上传失败' : '上架成功但图片上传失败');
        closeAddToolModal();
        loadToolList(document.getElementById('searchKeyword').value);
    });
}

function deleteToolImage(toolCode) {
    if (!confirm('确定要删除该工具的图片吗？')) return;

    const token = sessionStorage.getItem(AUTH_TOKEN_KEY);
    fetch(API_BASE_URL + '/admin/tool/image/' + toolCode, {
        method: 'DELETE',
        headers: { [AUTH_HEADER]: token }
    })
    .then(response => response.json())
    .then(data => {
        if (data.code === 200) {
            alert('图片已删除');
            loadToolList(document.getElementById('searchKeyword').value);
        } else {
            alert(data.message || '删除失败');
        }
    });
}

function offlineTool(toolCode) {
    if (!confirm('确定要下架该工具吗？')) return;
    updateToolStatus(toolCode, 'OFFLINE');
}

function startMaintenance(toolCode) {
    if (!confirm('确定要将该工具设为维护中吗？')) return;
    updateToolStatus(toolCode, 'MAINTENANCE');
}

function finishMaintenance(toolCode) {
    if (!confirm('确定要完成维护吗？')) return;
    updateToolStatus(toolCode, 'AVAILABLE');
}

function updateToolStatus(toolCode, status) {
    const token = sessionStorage.getItem(AUTH_TOKEN_KEY);
    fetch(API_BASE_URL + '/admin/tool/status/' + toolCode + '?status=' + status, {
        method: 'PUT',
        headers: { [AUTH_HEADER]: token }
    })
    .then(response => response.json())
    .then(data => {
        if (data.code === 200) {
            alert('操作成功');
            loadToolList(document.getElementById('searchKeyword').value);
        } else {
            alert(data.message || '操作失败');
        }
    });
}

function searchTool() {
    const keyword = document.getElementById('searchKeyword').value;
    toolPageNum = 1;
    loadToolList(keyword);
}

// ==================== 柜子初始化功能 ====================

async function initCabinet() {
    try {
        document.getElementById('initStatus').textContent = '初始化中...';
        updateOperationLog('正在启动摄像头...');

        const startResp = await fetch(API_BASE_URL + '/tool/cabinet/camera/start', {
            method: 'POST',
            headers: { [AUTH_HEADER]: sessionStorage.getItem(AUTH_TOKEN_KEY) }
        });
        const startData = await startResp.json();

        if (startData.code !== 200) {
            alert('摄像头启动失败: ' + (startData.message || '未知错误'));
            document.getElementById('initStatus').textContent = '初始化失败';
            return;
        }

        cameraRunning = true;
        document.getElementById('videoContainer').style.display = 'block';
        document.getElementById('btnInitCabinet').disabled = true;
        document.getElementById('btnConfirmInit').disabled = false;
        document.getElementById('btnCancelInit').style.display = 'inline-block';

        startFramePolling();

        updateOperationLog('摄像头已启动，请调整摄像头位置，实时检测结果将显示在右侧');

    } catch (err) {
        console.error('初始化失败:', err);
        alert('初始化失败，请检查后端服务');
        document.getElementById('initStatus').textContent = '初始化失败';
        await stopCamera();
    }
}

let initDetectedTools = [];

async function confirmInit() {
    try {
        const frameResp = await fetch(API_BASE_URL + '/tool/cabinet/frame-info', {
            method: 'GET',
            headers: { [AUTH_HEADER]: sessionStorage.getItem(AUTH_TOKEN_KEY) }
        });
        const frameData = await frameResp.json();

        if (frameData.code !== 200) {
            alert('获取检测结果失败: ' + (frameData.message || '未知错误'));
            return;
        }

        const detectedTools = frameData.data?.detections || [];
        if (detectedTools.length === 0) {
            alert('未检测到任何工具，请确保工具在摄像头视野内');
            return;
        }

        initDetectedTools = detectedTools;
        const toolCodes = detectedTools.map((d, idx) => d.label);
        const positions = detectedTools.map((_, idx) => {
            const row = String.fromCharCode(65 + Math.floor(idx / 3));
            const col = (idx % 3) + 1;
            return row + col;
        });

        document.getElementById('initStatus').textContent = '正在初始化...';
        updateOperationLog('检测到 ' + toolCodes.length + ' 个工具，正在提交...');

        const initResp = await fetch(API_BASE_URL + '/tool/cabinet/init', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                [AUTH_HEADER]: sessionStorage.getItem(AUTH_TOKEN_KEY)
            },
            body: JSON.stringify({
                toolCodes: toolCodes,
                positions: positions
            })
        });

        const initData = await initResp.json();

        if (initData.code === 200) {
            cabinetInitialized = true;
            document.getElementById('initStatus').textContent = '已初始化 - ' + toolCodes.length + '个位置';
            document.getElementById('btnInitCabinet').disabled = false;
            document.getElementById('btnConfirmInit').disabled = true;
            document.getElementById('btnCancelInit').style.display = 'none';
            updateOperationLog('柜子已初始化，共' + toolCodes.length + '个位置: ' + toolCodes.join(', '));
            loadInventory();
        } else {
            alert('初始化失败: ' + (initData.message || '未知错误'));
            document.getElementById('initStatus').textContent = '初始化失败';
        }

        await stopCamera();

    } catch (err) {
        console.error('初始化失败:', err);
        alert('初始化失败，请检查后端服务');
        document.getElementById('initStatus').textContent = '初始化失败';
        await stopCamera();
    }
}

async function cancelInit() {
    document.getElementById('btnInitCabinet').disabled = false;
    document.getElementById('btnConfirmInit').disabled = true;
    document.getElementById('btnCancelInit').style.display = 'none';
    document.getElementById('initStatus').textContent = '未初始化';
    document.getElementById('inventoryPanel').style.display = 'none';
    updateOperationLog('取消初始化');
    await stopCamera();
}

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
                    const statusText = item.status === '0' ? '在柜中' : '空位';
                    const statusColor = item.status === '0' ? '#2ecc71' : '#e74c3c';
                    tr.innerHTML = `
                        <td style="padding: 8px; border: 1px solid #ddd; text-align: center;">${item.position}</td>
                        <td style="padding: 8px; border: 1px solid #ddd; text-align: center;">${item.toolCode}</td>
                        <td style="padding: 8px; border: 1px solid #ddd; text-align: center;">${item.toolCodeName || '-'}</td>
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
            updateOperationLog('摄像头已启动');
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

function updateDetectionDisplay(data) {
    const currentDetectedTools = data.detectedTools || [];

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

function getToolChineseName(toolCode) {
    if (!toolCode) return '';
    const labelMap = {
        'wrench': '扳手',
        'screwdriver': '螺丝刀',
        'pliers': '钳子'
    };
    return labelMap[toolCode] || toolCode;
}

function updateOperationLog(message) {
    const logEl = document.getElementById('operationLog');
    if (logEl) {
        const timestamp = new Date().toLocaleTimeString();
        logEl.innerHTML = '[' + timestamp + '] ' + message;
    }
}

document.addEventListener('DOMContentLoaded', async function() {
    loadToolList(document.getElementById('searchKeyword').value);

    const searchInput = document.getElementById('searchKeyword');
    if (searchInput) {
        searchInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                searchTool();
            }
        });
    }

    // 检查柜子初始化状态
    try {
        const response = await fetch(API_BASE_URL + '/tool/cabinet/inventory', {
            method: 'GET',
            headers: { [AUTH_HEADER]: sessionStorage.getItem(AUTH_TOKEN_KEY) }
        });
        const data = await response.json();

        if (data.code === 200 && data.data && data.data.length > 0) {
            cabinetInitialized = true;
            document.getElementById('initStatus').textContent = '已初始化 - ' + data.data.length + '个位置';
            updateOperationLog('检测到柜子已初始化，共' + data.data.length + '个位置');
            loadInventory();
        }
    } catch (err) {
        console.log('检查柜子状态失败:', err);
    }
});